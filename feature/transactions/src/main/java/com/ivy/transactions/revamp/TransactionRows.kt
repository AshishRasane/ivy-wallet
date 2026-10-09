package com.ivy.transactions.revamp

import androidx.compose.runtime.Immutable
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.design.revamp.AmountFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class AmountTone { INCOME, EXPENSE, NEUTRAL }

enum class TransactionFilter { ALL, EXPENSES, INCOME }

@Suppress("DataClassTypedIDs") // UI model; the legacy edit screen takes a UUID
@Immutable
data class TransactionRowUi(
    val id: UUID,
    val type: TransactionType,
    val title: String,
    /** e.g. "Food & Drinks · HDFC Savings" or "HDFC Savings → Cash" */
    val subtitle: String,
    val initial: String,
    val amount: String,
    val tone: AmountTone,
    /** ARGB of the category (or account) color, used as a soft avatar background. */
    val avatarColor: Int?,
)

@Immutable
data class TransactionDayGroupUi(
    /** "Today", "Yesterday", "Mon, 6 Oct" */
    val label: String,
    /** Income minus expenses of the day; null when amounts are in different currencies. */
    val total: String?,
    val totalTone: AmountTone,
    val rows: ImmutableList<TransactionRowUi>,
)

/**
 * Turns domain transactions into day groups for the revamped lists (Home "Recent", Transactions tab).
 * Pure: no Android or database access, so it's unit-tested.
 */
object TransactionRows {
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val dayWithYearFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

    @Suppress("LongParameterList")
    fun group(
        transactions: List<Transaction>,
        accounts: Map<AccountId, Account>,
        categories: Map<CategoryId, Category>,
        baseCurrency: String,
        zone: ZoneId,
        today: LocalDate,
        filter: TransactionFilter = TransactionFilter.ALL,
        limit: Int? = null,
    ): ImmutableList<TransactionDayGroupUi> {
        val visible = transactions
            .filter { it.settled && filter.matches(it) }
            .sortedByDescending { it.time }
            .let { if (limit != null) it.take(limit) else it }

        return visible
            .groupBy { it.time.atZone(zone).toLocalDate() }
            .map { (date, dayTransactions) ->
                val total = dayTotal(dayTransactions, baseCurrency)
                TransactionDayGroupUi(
                    label = dayLabel(date, today),
                    total = total?.let { AmountFormat.format(it, baseCurrency, signed = true) },
                    totalTone = when {
                        total == null || total == 0.0 -> AmountTone.NEUTRAL
                        total > 0 -> AmountTone.INCOME
                        else -> AmountTone.EXPENSE
                    },
                    rows = dayTransactions.map { it.toRow(accounts, categories) }.toImmutableList(),
                )
            }
            .toImmutableList()
    }

    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(if (date.year == today.year) dayFormat else dayWithYearFormat)
    }

    private fun TransactionFilter.matches(trn: Transaction): Boolean = when (this) {
        TransactionFilter.ALL -> true
        TransactionFilter.EXPENSES -> trn is Expense
        TransactionFilter.INCOME -> trn is Income
    }

    /** Transfers don't change the total; mixed currencies can't be summed without rates. */
    private fun dayTotal(transactions: List<Transaction>, baseCurrency: String): Double? {
        val flows = transactions.mapNotNull {
            when (it) {
                is Income -> it.value.asset.code to it.value.amount.value
                is Expense -> it.value.asset.code to -it.value.amount.value
                is Transfer -> null
            }
        }
        return flows.takeIf { list -> list.all { (currency, _) -> currency == baseCurrency } }
            ?.sumOf { (_, amount) -> amount }
    }

    private fun Transaction.toRow(
        accounts: Map<AccountId, Account>,
        categories: Map<CategoryId, Category>,
    ): TransactionRowUi {
        val category = category?.let { categories[it] }
        val categoryName = category?.name?.value
        return when (this) {
            is Expense -> flowRow(
                type = TransactionType.EXPENSE,
                fallbackTitle = categoryName ?: "Expense",
                subtitle = listOfNotNull(categoryName, accounts[account]?.name?.value).joinToString(" · "),
                amount = AmountFormat.format(-value.amount.value, value.asset.code),
                tone = AmountTone.EXPENSE,
                avatarColor = category?.color?.value ?: accounts[account]?.color?.value,
            )

            is Income -> flowRow(
                type = TransactionType.INCOME,
                fallbackTitle = categoryName ?: "Income",
                subtitle = listOfNotNull(categoryName, accounts[account]?.name?.value).joinToString(" · "),
                amount = AmountFormat.format(value.amount.value, value.asset.code, signed = true),
                tone = AmountTone.INCOME,
                avatarColor = category?.color?.value ?: accounts[account]?.color?.value,
            )

            is Transfer -> flowRow(
                type = TransactionType.TRANSFER,
                fallbackTitle = "Transfer",
                subtitle = "${accounts[fromAccount]?.name?.value ?: "?"} → ${accounts[toAccount]?.name?.value ?: "?"}",
                amount = AmountFormat.format(fromValue.amount.value, fromValue.asset.code),
                tone = AmountTone.NEUTRAL,
                avatarColor = accounts[fromAccount]?.color?.value,
            )
        }
    }

    @Suppress("LongParameterList")
    private fun Transaction.flowRow(
        type: TransactionType,
        fallbackTitle: String,
        subtitle: String,
        amount: String,
        tone: AmountTone,
        avatarColor: Int?,
    ): TransactionRowUi {
        val title = title?.value ?: fallbackTitle
        return TransactionRowUi(
            id = id.value,
            type = type,
            title = title,
            subtitle = subtitle,
            initial = title.first().uppercase(),
            amount = amount,
            tone = tone,
            avatarColor = avatarColor,
        )
    }
}
