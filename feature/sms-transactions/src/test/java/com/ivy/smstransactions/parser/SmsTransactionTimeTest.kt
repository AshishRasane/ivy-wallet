package com.ivy.smstransactions.parser

import com.ivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SmsTransactionTimeTest {

    @Test
    fun `uses receive time when the SMS has no date`() {
        // When
        val res = trn(date = null).resolveDateTime(RECEIVED_AT, IST)

        // Then
        res shouldBe RECEIVED_AT
    }

    @Test
    fun `uses receive time when the SMS date is the same day`() {
        // When
        val res = trn(date = LocalDate.of(2026, 10, 1)).resolveDateTime(RECEIVED_AT, IST)

        // Then
        res shouldBe RECEIVED_AT
    }

    @Test
    fun `uses the SMS date and time for an earlier day`() {
        // When
        val res = trn(
            date = LocalDate.of(2026, 9, 28),
            time = LocalTime.of(9, 30),
        ).resolveDateTime(RECEIVED_AT, IST)

        // Then
        res shouldBe Instant.parse("2026-09-28T04:00:00Z")
    }

    @Test
    fun `defaults to noon when an earlier-day SMS has no time`() {
        // When
        val res = trn(date = LocalDate.of(2026, 9, 28)).resolveDateTime(RECEIVED_AT, IST)

        // Then
        res shouldBe Instant.parse("2026-09-28T06:30:00Z")
    }

    @Test
    fun `ignores future and implausibly old dates`() {
        // When
        val future = trn(date = LocalDate.of(2026, 10, 5)).resolveDateTime(RECEIVED_AT, IST)
        val tooOld = trn(date = LocalDate.of(2025, 1, 1)).resolveDateTime(RECEIVED_AT, IST)

        // Then
        future shouldBe RECEIVED_AT
        tooOld shouldBe RECEIVED_AT
    }

    companion object {
        private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

        // 2026-10-01 15:30 IST
        private val RECEIVED_AT: Instant = Instant.parse("2026-10-01T10:00:00Z")

        private fun trn(date: LocalDate?, time: LocalTime? = null) = SmsTransaction(
            type = TransactionType.EXPENSE,
            amount = 100.0,
            counterparty = null,
            accountEnding = null,
            bank = null,
            reference = null,
            date = date,
            time = time,
        )
    }
}
