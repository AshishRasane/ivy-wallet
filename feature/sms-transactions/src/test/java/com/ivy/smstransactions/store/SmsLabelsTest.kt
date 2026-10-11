package com.ivy.smstransactions.store

import io.kotest.matchers.shouldBe
import org.junit.Test

class SmsLabelsTest {
    @Test
    fun `digits show as an account ending, a card name as a card`() {
        accountLabel("1234") shouldBe "A/c XX1234"
        accountLabel("Scapia Federal Visa") shouldBe "Scapia Federal Visa card"
        accountLabel(null) shouldBe null
    }
}
