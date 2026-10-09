package com.ivy.smstransactions.store

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test

class SmsKeysTest {

    @Test
    fun `fingerprint uses the bank reference when there is one`() {
        // The same UPI transfer reported by two different senders is one transaction
        SmsKeys.fingerprint("412345678901", "AX-HDFCBK", "Rs 100 debited ...") shouldBe "ref:412345678901"
        SmsKeys.fingerprint("412345678901", "VM-PAYTMB", "Sent Rs.100 ...") shouldBe "ref:412345678901"
    }

    @Test
    fun `fingerprint without a reference depends on sender and text`() {
        val a = SmsKeys.fingerprint(null, "AX-HDFCBK", "Rs 300 spent at DMART")
        val sameAgain = SmsKeys.fingerprint(null, "ax-hdfcbk", "Rs 300  spent at DMART ")
        val other = SmsKeys.fingerprint(null, "AX-HDFCBK", "Rs 301 spent at DMART")

        a shouldStartWith "hash:"
        sameAgain shouldBe a
        other shouldNotBe a
    }

    @Test
    fun `account key combines bank and account ending`() {
        SmsKeys.accountKey("HDFC Bank", "1234") shouldBe "hdfc bank|1234"
        SmsKeys.accountKey(null, "1234") shouldBe "|1234"
        SmsKeys.accountKey("HDFC Bank", null) shouldBe null
    }

    @Test
    fun `merchant key normalizes names and UPI ids`() {
        SmsKeys.merchantKey("SWIGGY") shouldBe "swiggy"
        SmsKeys.merchantKey("  Swiggy ") shouldBe "swiggy"
        SmsKeys.merchantKey("zomato@paytm") shouldBe "zomato"
        SmsKeys.merchantKey("ACME CORP PVT. LTD") shouldBe "acme corp pvt ltd"
        SmsKeys.merchantKey("@@@") shouldBe null
        SmsKeys.merchantKey(null) shouldBe null
    }

    @Test
    fun `payee key is stored next to the account links`() {
        SmsKeys.payeeKey("CRED CCBP") shouldBe "payee|cred ccbp"
        SmsKeys.payeeKey("cred.club@axisb") shouldBe "payee|cred club"
        SmsKeys.payeeKey(null) shouldBe null
    }
}
