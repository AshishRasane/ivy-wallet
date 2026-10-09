package com.ivy.smstransactions.store

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.SmsDao
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.SmsAccountLinkEntity
import com.ivy.data.db.entity.SmsCategoryLinkEntity
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.data.db.entity.SmsTransactionStatus
import com.ivy.smstransactions.parser.InvestmentPayees
import com.ivy.smstransactions.parser.SmsTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A newly recorded SMS transaction plus what was learned from earlier choices. */
data class RecordedSmsTransaction(
    val entity: SmsTransactionEntity,
    val suggestion: SmsSuggestion,
)

/**
 * What the user chose last time for this SMS account / merchant: the account, and either
 * the category or, for a transfer, the account the money goes to.
 */
@Suppress("DataClassTypedIDs", "DataClassDefaultValues") // ids of Room entities; transfer-only fields
data class SmsSuggestion(
    /** The SMS's type, or a transfer when the user saved this merchant as one before. */
    val type: TransactionType,
    val accountId: UUID?,
    val accountName: String?,
    val categoryId: UUID?,
    val categoryName: String?,
    val toAccountId: UUID? = null,
    val toAccountName: String? = null,
) {
    /** Everything is known, so the transaction can be saved without opening the app. */
    val isComplete: Boolean
        get() = if (type == TransactionType.TRANSFER) {
            accountId != null && toAccountId != null && accountId != toAccountId
        } else {
            accountId != null && categoryId != null
        }
}

/**
 * Keeps detected SMS transactions until the user adds or ignores them,
 * and learns which account and category they belong to.
 */
@Singleton
class SmsTransactionStore @Inject constructor(
    private val dao: SmsDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val timeProvider: TimeProvider,
) {
    val pending: Flow<List<SmsTransactionEntity>> = dao.observeByStatus(SmsTransactionStatus.PENDING)

    /**
     * @return null when this transaction was already seen (duplicate SMS)
     * or is already saved as a transaction.
     */
    suspend fun record(
        trn: SmsTransaction,
        sender: String,
        body: String,
        dateTime: Instant,
    ): RecordedSmsTransaction? {
        val reference = trn.reference
        val alreadySaved = reference != null && dao.isReferenceInTransactions(reference)
        val entity = SmsTransactionEntity(
            id = UUID.randomUUID(),
            fingerprint = SmsKeys.fingerprint(reference, sender, body),
            reference = reference,
            type = trn.type,
            amount = trn.amount,
            counterparty = trn.counterparty,
            accountEnding = trn.accountEnding,
            bank = trn.bank,
            dateTime = dateTime,
            status = SmsTransactionStatus.PENDING,
            createdAt = timeProvider.utcNow(),
        )
        val isNew = !alreadySaved && dao.insert(entity) != DUPLICATE
        return if (isNew) RecordedSmsTransaction(entity = entity, suggestion = suggestion(entity)) else null
    }

    suspend fun findById(id: UUID): SmsTransactionEntity? = dao.findById(id)

    /**
     * Only accounts and categories that still exist are suggested
     * (the user may have deleted one since it was learned).
     */
    suspend fun suggestion(entity: SmsTransactionEntity): SmsSuggestion {
        val account = SmsKeys.accountKey(entity.bank, entity.accountEnding)
            ?.let { dao.findAccountLink(it) }
            ?.let { accountDao.findById(it.accountId) }
        val payeeLink = SmsKeys.payeeKey(entity.counterparty)?.let { dao.findAccountLink(it) }
        // the user saved this merchant as an expense: that wins over the parser and name matching
        val notATransfer = payeeLink?.accountId == NOT_A_TRANSFER
        val isInvestment = InvestmentPayees.matches(entity.counterparty) && !notATransfer
        val payeeAccount = payeeAccount(entity, payeeLink, notATransfer, isInvestment)
        val type = suggestedType(entity.type, notATransfer, hasPayeeAccount = payeeAccount != null, isInvestment)
        if (type == TransactionType.TRANSFER) {
            val incoming = entity.type == TransactionType.INCOME
            val from = if (incoming) payeeAccount else account
            val to = if (incoming) account else payeeAccount
            return SmsSuggestion(
                type = type,
                accountId = from?.id,
                accountName = from?.name,
                categoryId = null,
                categoryName = null,
                toAccountId = to?.id,
                toAccountName = to?.name,
            )
        }
        val category = SmsKeys.merchantKey(entity.counterparty)
            ?.let { dao.findCategoryLink(it) }
            ?.let { categoryDao.findById(it.categoryId) }
        return SmsSuggestion(
            type = type,
            accountId = account?.id,
            accountName = account?.name,
            categoryId = category?.id,
            categoryName = category?.name,
        )
    }

    /**
     * Marks the SMS transaction as added and remembers what the user chose, so the next SMS
     * from the same account/merchant is pre-filled: the account, and either the category or,
     * when saved as a transfer from [accountId] to [toAccountId], the merchant's account.
     */
    suspend fun markAdded(id: UUID, accountId: UUID, categoryId: UUID?, toAccountId: UUID?) {
        val entity = dao.findById(id) ?: return
        dao.updateStatus(id, SmsTransactionStatus.ADDED)
        val now = timeProvider.utcNow()
        // money that came in (e.g. from Zerodha) and was saved as a transfer: the bank is the "to" side
        val incomingTransfer = entity.type == TransactionType.INCOME && toAccountId != null
        val bankAccountId = if (incomingTransfer && toAccountId != null) toAccountId else accountId
        val payeeAccountId = if (incomingTransfer) accountId else toAccountId
        SmsKeys.accountKey(entity.bank, entity.accountEnding)?.let { key ->
            dao.saveAccountLink(SmsAccountLinkEntity(key = key, accountId = bankAccountId, updatedAt = now))
        }
        val payee = SmsKeys.payeeKey(entity.counterparty)
        if (payee != null && (entity.type != TransactionType.INCOME || incomingTransfer)) {
            if (payeeAccountId != null) {
                dao.saveAccountLink(SmsAccountLinkEntity(key = payee, accountId = payeeAccountId, updatedAt = now))
            } else {
                // saved as an expense this time: stop suggesting a transfer for this merchant
                dao.saveAccountLink(SmsAccountLinkEntity(key = payee, accountId = NOT_A_TRANSFER, updatedAt = now))
            }
        }
        val merchant = SmsKeys.merchantKey(entity.counterparty)
        if (merchant != null && categoryId != null && toAccountId == null) {
            dao.saveCategoryLink(
                SmsCategoryLinkEntity(merchantKey = merchant, categoryId = categoryId, updatedAt = now)
            )
        }
    }

    /** The account the user saved this merchant's transfers to, else one named like the investment app. */
    private suspend fun payeeAccount(
        entity: SmsTransactionEntity,
        payeeLink: SmsAccountLinkEntity?,
        notATransfer: Boolean,
        isInvestment: Boolean,
    ): AccountEntity? = when {
        notATransfer -> null
        payeeLink != null -> accountDao.findById(payeeLink.accountId)
        isInvestment -> accountNamedLike(InvestmentPayees.brand(entity.counterparty))
        else -> null
    }

    private fun suggestedType(
        parsed: TransactionType,
        notATransfer: Boolean,
        hasPayeeAccount: Boolean,
        isInvestment: Boolean,
    ): TransactionType = when {
        notATransfer && parsed == TransactionType.TRANSFER -> TransactionType.EXPENSE
        parsed == TransactionType.TRANSFER -> TransactionType.TRANSFER
        parsed == TransactionType.EXPENSE && hasPayeeAccount -> TransactionType.TRANSFER
        // money back from an investment app (a sale or redemption)
        parsed == TransactionType.INCOME && isInvestment -> TransactionType.TRANSFER
        else -> parsed
    }

    /** The account whose name contains the investment app's name, e.g. "Kuvera" or "Zerodha (stocks)". */
    private suspend fun accountNamedLike(brand: String?): AccountEntity? {
        brand ?: return null
        return accountDao.findAll().firstOrNull { it.name.contains(brand, ignoreCase = true) }
    }

    suspend fun markIgnored(id: UUID) {
        dao.updateStatus(id, SmsTransactionStatus.IGNORED)
    }

    /** Pending items expire after 30 days; added/ignored ones are kept 90 days for duplicate checks. */
    suspend fun cleanUp() {
        val now = timeProvider.utcNow()
        dao.deleteOlderThan(SmsTransactionStatus.PENDING, now.minus(PENDING_TTL).toEpochMilli())
        dao.deleteOlderThan(SmsTransactionStatus.ADDED, now.minus(HISTORY_TTL).toEpochMilli())
        dao.deleteOlderThan(SmsTransactionStatus.IGNORED, now.minus(HISTORY_TTL).toEpochMilli())
    }

    private companion object {
        const val DUPLICATE = -1L

        /** Stored as a merchant's account when the user chose "not a transfer" for it. */
        val NOT_A_TRANSFER = UUID(0L, 0L)
        val PENDING_TTL: Duration = Duration.ofDays(30)
        val HISTORY_TTL: Duration = Duration.ofDays(90)
    }
}
