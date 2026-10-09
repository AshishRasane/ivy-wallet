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
        val accountId = ensureNotNull(suggestion.accountId) { "Account not learned yet" }
        val categoryId = ensureNotNull(suggestion.categoryId) { "Category not learned yet" }
        val account = ensureNotNull(accountDao.findById(accountId)) { "Account no longer exists" }

        val currency = account.currency ?: settingsDao.findFirst().currency
        val value = PositiveValue(
            amount = PositiveDouble.from(sms.amount).bind(),
            asset = AssetCode.from(currency).bind(),
        )
        val transaction = sms.toTransaction(value, AccountId(accountId), CategoryId(categoryId))

        transactionRepository.save(transaction)
        store.markAdded(sms.id, accountId, categoryId)
        refreshWidget(WalletBalanceWidgetReceiver::class.java)
        QuickAdded(sms = sms, transaction = transaction, suggestion = suggestion)
    }

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
