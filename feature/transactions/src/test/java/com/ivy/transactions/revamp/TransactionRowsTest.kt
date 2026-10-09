package com.ivy.transactions.revamp

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class TransactionRowsTest {

    @Test
    fun `groups by local day, newest first, with labels and totals`() {
        // Given - 23:30 UTC on Oct 8 is already Oct 9 in India
        val trns = listOf(
            expense(500.0, "2026-10-09T05:00:00Z", title = "Swiggy"),
            income(85000.0, "2026-10-08T23:30:00Z", title = "Salary"),
            expense(386.0, "2026-10-08T10:00:00Z", title = "Uber"),
        )

        // When
        val groups = group(trns)

        // Then
        groups.map { it.label } shouldBe listOf("Today", "Yesterday")
        groups[0].rows.map { it.title } shouldBe listOf("Swiggy", "Salary")
        groups[0].total shouldBe "+₹84,500.00"
        groups[0].totalTone shouldBe AmountTone.INCOME
        groups[1].total shouldBe "−₹386.00"
        groups[1].totalTone shouldBe AmountTone.EXPENSE
    }

    @Test
    fun `rows show category and account, signed amounts`() {
        val rows = group(
            listOf(
                expense(2499.0, "2026-10-09T05:00:00Z", title = "Swiggy"),
                income(500.0, "2026-10-09T04:00:00Z", title = null),
            )
        ).single().rows

        rows[0].subtitle shouldBe "Food & Drinks · HDFC Savings"
        rows[0].amount shouldBe "−₹2,499.00"
        rows[0].initial shouldBe "S"
        rows[1].title shouldBe "Food & Drinks"
        rows[1].amount shouldBe "+₹500.00"
    }

    @Test
    fun `filters expenses or income and skips planned (unsettled) transactions`() {
        val trns = listOf(
            expense(100.0, "2026-10-09T05:00:00Z", title = "A"),
            income(200.0, "2026-10-09T04:00:00Z", title = "B"),
            expense(300.0, "2026-10-09T03:00:00Z", title = "Planned", settled = false),
        )

        group(trns, TransactionFilter.EXPENSES).single().rows.map { it.title } shouldBe listOf("A")
        group(trns, TransactionFilter.INCOME).single().rows.map { it.title } shouldBe listOf("B")
        group(trns).single().rows.map { it.title } shouldBe listOf("A", "B")
    }

    @Test
    fun `transfers are neutral and excluded from the day total`() {
        val groups = group(
            listOf(
                transfer(1000.0, "2026-10-09T05:00:00Z"),
                expense(100.0, "2026-10-09T04:00:00Z", title = "Tea"),
            )
        )

        val transferRow = groups.single().rows.first()
        transferRow.subtitle shouldBe "HDFC Savings → Cash"
        transferRow.tone shouldBe AmountTone.NEUTRAL
        groups.single().total shouldBe "−₹100.00"
    }

    @Test
    fun `day total is hidden for mixed currencies`() {
        val groups = group(
            listOf(
                expense(10.0, "2026-10-09T05:00:00Z", title = "Coffee", currency = "USD"),
                expense(100.0, "2026-10-09T04:00:00Z", title = "Tea"),
            )
        )

        groups.single().total shouldBe null
    }

    @Test
    fun `limit keeps only the most recent transactions`() {
        val trns = (1..8).map { expense(it.toDouble(), "2026-10-0${it}T05:00:00Z", title = "T$it") }

        val rows = group(trns, limit = 3).flatMap { it.rows }

        rows.map { it.title } shouldBe listOf("T8", "T7", "T6")
    }

    @Test
    fun `older days show weekday and date`() {
        TransactionRows.dayLabel(LocalDate.of(2026, 10, 5), TODAY) shouldBe "Mon, 5 Oct"
        TransactionRows.dayLabel(LocalDate.of(2025, 12, 31), TODAY) shouldBe "Wed, 31 Dec 2025"
    }

    companion object {
        private val IST: ZoneId = ZoneId.of("Asia/Kolkata")
        private val TODAY: LocalDate = LocalDate.of(2026, 10, 9)
        private val HDFC = AccountId(UUID.randomUUID())
        private val CASH = AccountId(UUID.randomUUID())
        private val FOOD = CategoryId(UUID.randomUUID())

        private val accounts = mapOf(HDFC to account("HDFC Savings"), CASH to account("Cash"))
        private val categories = mapOf(FOOD to category("Food & Drinks"))

        private fun group(
            trns: List<Transaction>,
            filter: TransactionFilter = TransactionFilter.ALL,
            limit: Int? = null,
        ) = TransactionRows.group(
            transactions = trns,
            accounts = accounts,
            categories = categories,
            baseCurrency = "INR",
            zone = IST,
            today = TODAY,
            filter = filter,
            limit = limit,
        )

        private fun account(name: String): Account = mockk {
            every { this@mockk.name } returns NotBlankTrimmedString.unsafe(name)
            every { color } returns ColorInt(0xFF1F4FA8.toInt())
        }

        private fun category(name: String): Category = mockk {
            every { this@mockk.name } returns NotBlankTrimmedString.unsafe(name)
            every { color } returns ColorInt(0xFFA3410B.toInt())
        }

        private fun value(amount: Double, currency: String) =
            PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe(currency))

        private val metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
        )

        private fun expense(
            amount: Double,
            time: String,
            title: String?,
            settled: Boolean = true,
            currency: String = "INR",
        ) = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = title?.let { NotBlankTrimmedString.unsafe(it) },
            description = null,
            category = FOOD,
            time = Instant.parse(time),
            settled = settled,
            metadata = metadata,
            tags = emptyList(),
            value = value(amount, currency),
            account = HDFC,
        )

        private fun income(amount: Double, time: String, title: String?) = Income(
            id = TransactionId(UUID.randomUUID()),
            title = title?.let { NotBlankTrimmedString.unsafe(it) },
            description = null,
            category = FOOD,
            time = Instant.parse(time),
            settled = true,
            metadata = metadata,
            tags = emptyList(),
            value = value(amount, "INR"),
            account = HDFC,
        )

        private fun transfer(amount: Double, time: String) = Transfer(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = Instant.parse(time),
            settled = true,
            metadata = metadata,
            tags = emptyList(),
            fromAccount = HDFC,
            fromValue = value(amount, "INR"),
            toAccount = CASH,
            toValue = value(amount, "INR"),
        )
    }
}
