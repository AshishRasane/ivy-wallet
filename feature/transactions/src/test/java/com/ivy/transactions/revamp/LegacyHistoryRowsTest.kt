package com.ivy.transactions.revamp

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.data.TransactionHistoryDateDivider
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class LegacyHistoryRowsTest {

    @Test
    fun `each date divider starts a day group with its total`() {
        // Given
        val history = listOf(
            TransactionHistoryDateDivider(date = TODAY, income = 0.0, expenses = 2499.0),
            transaction(TransactionType.EXPENSE, 2499.0, title = "Swiggy"),
            TransactionHistoryDateDivider(date = TODAY.minusDays(1), income = 85000.0, expenses = 386.0),
            transaction(TransactionType.INCOME, 85000.0, title = "Salary"),
            transaction(TransactionType.EXPENSE, 386.0, title = null),
        )

        // When
        val groups = group(history, focusAccountId = HDFC.id)

        // Then
        groups.map { it.label } shouldBe listOf("Today", "Yesterday")
        groups[0].total shouldBe "−₹2,499.00"
        groups[0].totalTone shouldBe AmountTone.EXPENSE
        groups[1].total shouldBe "+₹84,614.00"
        groups[1].rows.map { it.title } shouldBe listOf("Salary", "Food & Drinks")
        groups[1].rows[1].subtitle shouldBe "Food & Drinks · HDFC Savings"
        groups[1].rows[1].amount shouldBe "−₹386.00"
    }

    @Test
    fun `transfers are money out or in for the account of the screen`() {
        // Given
        val transfer = transaction(TransactionType.TRANSFER, 5000.0, title = null, toAccountId = CASH.id)
        val history = listOf(TransactionHistoryDateDivider(TODAY, income = 0.0, expenses = 0.0), transfer)

        // When / Then
        group(history, focusAccountId = HDFC.id).single().rows.single().run {
            amount shouldBe "−₹5,000.00"
            tone shouldBe AmountTone.EXPENSE
            subtitle shouldBe "HDFC Savings → Cash"
        }
        group(history, focusAccountId = CASH.id).single().rows.single().run {
            amount shouldBe "+₹5,000.00"
            tone shouldBe AmountTone.INCOME
        }
        group(history, focusAccountId = null).single().rows.single().tone shouldBe AmountTone.NEUTRAL
    }

    @Test
    fun `days without transactions are left out`() {
        val history = listOf(TransactionHistoryDateDivider(TODAY, income = 0.0, expenses = 0.0))

        group(history, focusAccountId = null) shouldBe emptyList()
    }

    companion object {
        private val TODAY = LocalDate.of(2026, 10, 9)
        private val HDFC = Account(name = "HDFC Savings", color = 0xFF1F4FA8.toInt(), currency = "INR")
        private val CASH = Account(name = "Cash", color = 0xFF0B7A55.toInt(), currency = "INR")
        private val FOOD_ID = UUID.randomUUID()
        private val FOOD: Category = mockk {
            every { id } returns CategoryId(FOOD_ID)
            every { name } returns NotBlankTrimmedString.unsafe("Food & Drinks")
            every { color } returns ColorInt(0xFFA3410B.toInt())
        }

        private fun transaction(
            type: TransactionType,
            amount: Double,
            title: String?,
            toAccountId: UUID? = null,
        ) = Transaction(
            accountId = HDFC.id,
            type = type,
            amount = BigDecimal.valueOf(amount),
            toAccountId = toAccountId,
            title = title,
            categoryId = FOOD_ID.takeIf { type != TransactionType.TRANSFER },
        )

        private fun group(history: List<Any>, focusAccountId: UUID?) = LegacyHistoryRows.group(
            history = history.filterIsInstance<com.ivy.base.legacy.TransactionHistoryItem>(),
            accounts = listOf(HDFC, CASH),
            categories = listOf(FOOD),
            baseCurrency = "INR",
            today = TODAY,
            focusAccountId = focusAccountId,
        )
    }
}
