package com.ivy.transactions.revamp

import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.design.revamp.AmountFormat
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.util.UUID

/**
 * Turns the legacy history (date dividers followed by their transactions), as loaded by the
 * account / category screen, into day groups for the revamped list.
 * Pure: no Android or database access, so it's unit-tested.
 */
object LegacyHistoryRows {

    /**
     * @param focusAccountId the account whose screen this is: its transfers show as money
     * in (+) or out (−) instead of a neutral amount.
     */
    @Suppress("LongParameterList")
    fun group(
        history: List<TransactionHistoryItem>,
        accounts: List<Account>,
        categories: List<Category>,
        baseCurrency: String,
        today: LocalDate,
        focusAccountId: UUID?,
    ): ImmutableList<TransactionDayGroupUi> {
        val accountsById = accounts.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id.value }
        val groups = mutableListOf<TransactionDayGroupUi>()
        var rows = mutableListOf<TransactionRowUi>()
        var divider: TransactionHistoryDateDivider? = null

        fun closeDay() {
            val day = divider
            if (day != null && rows.isNotEmpty()) groups += dayGroup(day, rows, baseCurrency, today)
            rows = mutableListOf()
        }

        history.forEach { item ->
            when (item) {
                is TransactionHistoryDateDivider -> {
                    closeDay()
                    divider = item
                }

                is Transaction -> rows += item.toRow(accountsById, categoriesById, baseCurrency, focusAccountId)
            }
        }
        closeDay()
        return groups.toImmutableList()
    }

    private fun Transaction.amountAndTone(
        currency: String,
        toCurrency: String,
        focusAccountId: UUID?,
    ): Pair<String, AmountTone> = when (type) {
        TransactionType.INCOME -> AmountFormat.format(amount.toDouble(), currency, signed = true) to AmountTone.INCOME
        TransactionType.EXPENSE -> AmountFormat.format(-amount.toDouble(), currency) to AmountTone.EXPENSE
        TransactionType.TRANSFER -> when (focusAccountId) {
            toAccountId -> AmountFormat.format(toAmount.toDouble(), toCurrency, signed = true) to AmountTone.INCOME
            accountId -> AmountFormat.format(-amount.toDouble(), currency) to AmountTone.EXPENSE
            else -> AmountFormat.format(amount.toDouble(), currency) to AmountTone.NEUTRAL
        }
    }

    private fun TransactionType.label(): String = when (this) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

    private fun dayGroup(
        divider: TransactionHistoryDateDivider,
        rows: List<TransactionRowUi>,
        baseCurrency: String,
        today: LocalDate,
    ): TransactionDayGroupUi {
        val total = divider.income - divider.expenses
        return TransactionDayGroupUi(
            label = TransactionRows.dayLabel(divider.date, today),
            total = AmountFormat.format(total, baseCurrency, signed = true),
            totalTone = when {
                total > 0 -> AmountTone.INCOME
                total < 0 -> AmountTone.EXPENSE
                else -> AmountTone.NEUTRAL
            },
            rows = rows.toImmutableList(),
        )
    }

    private fun Transaction.toRow(
        accounts: Map<UUID, Account>,
        categories: Map<UUID, Category>,
        baseCurrency: String,
        focusAccountId: UUID?,
    ): TransactionRowUi {
        val account = accounts[accountId]
        val toAccount = toAccountId?.let { accounts[it] }
        val category = categoryId?.let { categories[it] }
        val currency = account?.currency ?: baseCurrency
        val toCurrency = toAccount?.currency ?: baseCurrency
        val categoryName = category?.name?.value

        val (amount, tone) = amountAndTone(currency, toCurrency, focusAccountId)
        val title = title?.takeIf { it.isNotBlank() } ?: categoryName ?: type.label()
        return TransactionRowUi(
            id = id,
            type = type,
            title = title,
            subtitle = if (type == TransactionType.TRANSFER) {
                "${account?.name ?: "?"} → ${toAccount?.name ?: "?"}"
            } else {
                listOfNotNull(categoryName, account?.name).joinToString(" · ")
            },
            initial = title.first().uppercase(),
            amount = amount,
            tone = tone,
            avatarColor = category?.color?.value ?: account?.color,
        )
    }
}
