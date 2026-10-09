package com.ivy.transaction.revamp

import io.kotest.matchers.shouldBe
import org.junit.Test

class AmountInputTest {

    private fun type(keys: String, maxDecimals: Int = 2): String =
        keys.fold("") { text, key -> AmountInput.press(text, key.toString(), maxDecimals) }

    @Test
    fun `digits and one decimal point`() {
        type("2499") shouldBe "2499"
        type("24.5") shouldBe "24.5"
        type("24..5") shouldBe "24.5"
        type(".5") shouldBe "0.5"
    }

    @Test
    fun `no more decimals than the currency allows`() {
        type("10.999") shouldBe "10.99"
        type("10.5", maxDecimals = 0) shouldBe "105"
    }

    @Test
    fun `leading zero is replaced and whole part is capped`() {
        type("007") shouldBe "7"
        type("1234567890123") shouldBe "123456789012"
    }

    @Test
    fun `delete removes the last character`() {
        AmountInput.press("24.5", AmountInput.DELETE, 2) shouldBe "24."
        AmountInput.press("", AmountInput.DELETE, 2) shouldBe ""
    }

    @Test
    fun `converts to and from the amount`() {
        AmountInput.toAmount("2499.") shouldBe 2499.0
        AmountInput.toAmount("") shouldBe 0.0
        AmountInput.fromAmount(2499.0) shouldBe "2499"
        AmountInput.fromAmount(2499.5) shouldBe "2499.5"
        AmountInput.fromAmount(0.0) shouldBe ""
    }

    @Test
    fun `display groups the whole part while typing`() {
        AmountInput.display("1245604", indianGrouping = true) shouldBe "12,45,604"
        AmountInput.display("1245604.5", indianGrouping = true) shouldBe "12,45,604.5"
        AmountInput.display("1245604", indianGrouping = false) shouldBe "1,245,604"
        AmountInput.display("2499.", indianGrouping = true) shouldBe "2,499."
        AmountInput.display("", indianGrouping = true) shouldBe "0"
    }
}
