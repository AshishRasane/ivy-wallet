package com.ivy.smstransactions.store

import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.SmsDao
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.entity.SmsAccountLinkEntity
import com.ivy.data.db.entity.SmsCategoryLinkEntity
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.data.db.entity.SmsTransactionStatus
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

/** The account and category the user chose last time for this SMS account / merchant. */
@Suppress("DataClassTypedIDs") // ids of Room entities
data class SmsSuggestion(
    val accountId: UUID?,
    val accountName: String?,
    val categoryId: UUID?,
    val categoryName: String?,
) {
    /** Both are known, so the transaction can be saved without opening the app. */
    val isComplete: Boolean get() = accountId != null && categoryId != null
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
        val category = SmsKeys.merchantKey(entity.counterparty)
            ?.let { dao.findCategoryLink(it) }
            ?.let { categoryDao.findById(it.categoryId) }
        return SmsSuggestion(
            accountId = account?.id,
            accountName = account?.name,
            categoryId = category?.id,
            categoryName = category?.name,
        )
    }

    /**
     * Marks the SMS transaction as added and remembers the account and category
     * the user chose, so the next SMS from the same account/merchant is pre-filled.
     */
    suspend fun markAdded(id: UUID, accountId: UUID, categoryId: UUID?) {
        val entity = dao.findById(id) ?: return
        dao.updateStatus(id, SmsTransactionStatus.ADDED)
        val now = timeProvider.utcNow()
        SmsKeys.accountKey(entity.bank, entity.accountEnding)?.let { key ->
            dao.saveAccountLink(SmsAccountLinkEntity(key = key, accountId = accountId, updatedAt = now))
        }
        val merchant = SmsKeys.merchantKey(entity.counterparty)
        if (merchant != null && categoryId != null) {
            dao.saveCategoryLink(
                SmsCategoryLinkEntity(merchantKey = merchant, categoryId = categoryId, updatedAt = now)
            )
        }
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
        val PENDING_TTL: Duration = Duration.ofDays(30)
        val HISTORY_TTL: Duration = Duration.ofDays(90)
    }
}
