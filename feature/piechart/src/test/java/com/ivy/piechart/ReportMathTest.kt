package com.ivy.piechart

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.YearMonth

class ReportMathTest {

    @Test
    fun `last months run oldest first and cross the year`() {
        ReportMath.lastMonths(YearMonth.of(2026, 2), count = 3) shouldBe listOf(
            YearMonth.of(2025, 12),
            YearMonth.of(2026, 1),
            YearMonth.of(2026, 2),
        )
    }

    @Test
    fun `comparison with the previous month`() {
        val september = YearMonth.of(2026, 9)

        ReportMath.comparison(28431.0, 32300.0, september) shouldBe "12% less than September"
        ReportMath.comparison(86512.0, 85300.0, september) shouldBe "1% more than September"
        ReportMath.comparison(100.0, 100.0, september) shouldBe "Same as September"
        ReportMath.comparison(100.0, 0.0, september) shouldBe null
    }

    @Test
    fun `trend bars are relative to the highest month`() {
        // Given
        val months = ReportMath.lastMonths(YearMonth.of(2026, 10), count = 3)

        // When
        val bars = ReportMath.trend(months, listOf(20000.0, 40000.0, 10000.0), "INR")

        // Then
        bars.map { it.month } shouldBe listOf("Aug", "Sep", "Oct")
        bars.map { it.fraction } shouldBe listOf(0.5f, 1f, 0.25f)
        bars.map { it.amount } shouldBe listOf("₹20k", "₹40k", "₹10k")
        bars.map { it.current } shouldBe listOf(false, false, true)
    }

    @Test
    fun `no spending gives empty bars`() {
        val months = ReportMath.lastMonths(YearMonth.of(2026, 10), count = 2)

        ReportMath.trend(months, listOf(0.0, 0.0), "INR").map { it.fraction } shouldBe listOf(0f, 0f)
    }

    @Test
    fun `short amounts use lakh and crore for rupees`() {
        ReportMath.short(850.0, "INR") shouldBe "₹850"
        ReportMath.short(31240.0, "INR") shouldBe "₹31.2k"
        ReportMath.short(125000.0, "INR") shouldBe "₹1.3L"
        ReportMath.short(25000000.0, "INR") shouldBe "₹2.5Cr"
        ReportMath.short(1500000.0, "usd") shouldBe "1.5M USD"
        ReportMath.short(1500.0, "USD", withCode = false) shouldBe "1.5k"
    }
}
