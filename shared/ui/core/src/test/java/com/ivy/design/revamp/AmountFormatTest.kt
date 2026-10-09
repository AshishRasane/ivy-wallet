package com.ivy.design.revamp

import io.kotest.matchers.shouldBe
import org.junit.Test

class AmountFormatTest {

    @Test
    fun `INR uses the rupee symbol and Indian grouping`() {
        AmountFormat.format(124560.4, "INR") shouldBe "₹1,24,560.40"
        AmountFormat.format(12345678.0, "INR") shouldBe "₹1,23,45,678.00"
        AmountFormat.format(999.0, "INR") shouldBe "₹999.00"
        AmountFormat.format(1000.0, "inr") shouldBe "₹1,000.00"
    }

    @Test
    fun `other currencies use international grouping and the code`() {
        AmountFormat.format(1234567.5, "USD") shouldBe "1,234,567.50 USD"
        AmountFormat.format(12.0, "eur") shouldBe "12.00 EUR"
    }

    @Test
    fun `signs use a real minus and optional plus`() {
        AmountFormat.format(-386.0, "INR") shouldBe "−₹386.00"
        AmountFormat.format(85000.0, "INR", signed = true) shouldBe "+₹85,000.00"
        AmountFormat.format(0.0, "INR", signed = true) shouldBe "₹0.00"
        AmountFormat.format(-2.5, "USD") shouldBe "−2.50 USD"
    }

    @Test
    fun `decimals can be hidden and are rounded half up`() {
        AmountFormat.format(2499.5, "INR", showDecimals = false) shouldBe "₹2,500"
        AmountFormat.format(10.005, "INR") shouldBe "₹10.01"
    }
}
