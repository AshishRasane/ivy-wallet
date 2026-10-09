package com.ivy.smstransactions.store

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ivy.base.legacy.refreshWidget
import com.ivy.base.model.TransactionType
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.data.db.entity.SmsTransactionStatus
import com.ivy.data.model.AccountId
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.TransactionRepository
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import java.util.UUID
import javax.inject.Inject

/** What was saved, for the confirmation notification. */
data class QuickAdded(
    val sms: SmsTransactionEntity,
    val transaction: Transaction,
    val suggestion: SmsSuggestion,
)

/**
 * Saves an SMS transaction straight from its notification, using the account and
 * category learned from the user's earlier choices.
 */
class SmsQuickAdd @Inject constructor(
    private val store: SmsTransactionStore,
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao,
    private val settingsDao: SettingsDao,
) {
    suspend fun add(smsTransactionId: UUID): Either<String, QuickAdded> = either {
        val sms = ensureNotNull(store.findById(smsTransactionId)) { "SMS transaction not found" }
        ensure(sms.status == SmsTransactionStatus.PENDING) { "Already ${sms.status.lowercase()}" }
        val suggestion = store.suggestion(sms)
        ensure(suggestion.isComplete) { "Account or category not learned yet" }
        val accountId = ensureNotNull(suggestion.accountId) { "Account not learned yet" }
        val account = ensureNotNull(accountDao.findById(accountId)) { "Account no longer exists" }
        val baseCurrency = settingsDao.findFirst().currency
        val value = positiveValue(sms.amount, account.currency ?: baseCurrency).bind()

        val toAccountId = suggestion.toAccountId
        val transaction = if (suggestion.type == TransactionType.TRANSFER && toAccountId != null) {
            val toAccount = ensureNotNull(accountDao.findById(toAccountId)) { "Account no longer exists" }
            // a transfer between currencies needs an exchange rate: the user enters it in the app
            ensure((toAccount.currency ?: baseCurrency) == (account.currency ?: baseCurrency)) {
                "Different currencies"
            }
            sms.toTransfer(value, AccountId(accountId), AccountId(toAccountId))
        } else {
            val categoryId = ensureNotNull(suggestion.categoryId) { "Category not learned yet" }
            sms.toTransaction(value, AccountId(accountId), CategoryId(categoryId))
        }

        transactionRepository.save(transaction)
        store.markAdded(sms.id, accountId, suggestion.categoryId, toAccountId.takeIf { transaction is Transfer })
        refreshWidget(WalletBalanceWidgetReceiver::class.java)
        QuickAdded(sms = sms, transaction = transaction, suggestion = suggestion)
    }

    private fun positiveValue(amount: Double, currency: String): Either<String, PositiveValue> = either {
        PositiveValue(
            amount = PositiveDouble.from(amount).bind(),
            asset = AssetCode.from(currency).bind(),
        )
    }

    private fun SmsTransactionEntity.toTransfer(
        value: PositiveValue,
        from: AccountId,
        to: AccountId,
    ): Transfer = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = counterparty?.let { NotBlankTrimmedString.from(it).getOrNull() },
        description = NotBlankTrimmedString.from(prefillDescription()).getOrNull(),
        category = null,
        time = dateTime,
        settled = true,
        metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
        ),
        tags = emptyList(),
        fromAccount = from,
        fromValue = value,
        toAccount = to,
        toValue = value,
    )

    private fun SmsTransactionEntity.toTransaction(
        value: PositiveValue,
        account: AccountId,
        category: CategoryId,
    ): Transaction {
        val id = TransactionId(UUID.randomUUID())
        val title = counterparty?.let { NotBlankTrimmedString.from(it).getOrNull() }
        val description = NotBlankTrimmedString.from(prefillDescription()).getOrNull()
        val metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
        )
        return if (type == TransactionType.INCOME) {
            Income(
                id = id, title = title, description = description, category = category,
                time = dateTime, settled = true, metadata = metadata, tags = emptyList(),
                value = value, account = account,
            )
        } else {
            Expense(
                id = id, title = title, description = description, category = category,
                time = dateTime, settled = true, metadata = metadata, tags = emptyList(),
                value = value, account = account,
            )
        }
    }
}
