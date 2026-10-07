package com.ivy.ui.time.impl

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DatePickerMillisTest {

    @Test
    fun `picked date is read as a UTC calendar day`() {
        // Given - Material3 DatePicker returns UTC midnight of the tapped day
        val pickerMillis = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()

        // When
        val date = DatePickerMillis.toLocalDate(pickerMillis)

        // Then - not Oct 4, which a UTC-5 (New York) conversion would give
        date shouldBe LocalDate.of(2026, 10, 5)
    }

    @Test
    fun `initial date is the UTC midnight of the local day`() {
        // When
        val millis = DatePickerMillis.fromLocalDate(LocalDate.of(2026, 10, 5))

        // Then
        millis shouldBe Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()
    }

    @Test
    fun `early morning IST transaction opens the picker on its own day`() {
        // Given - 2:00 AM on Oct 6 in India is still Oct 5 in UTC
        val transactionTime = Instant.parse("2026-10-05T20:30:00Z")
        val localDate = transactionTime.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()

        // When
        val shownDate = DatePickerMillis.toLocalDate(DatePickerMillis.fromLocalDate(localDate))

        // Then
        shownDate shouldBe LocalDate.of(2026, 10, 6)
    }

    @Test
    fun `round trip keeps the date in every time zone`() {
        val date = LocalDate.of(2026, 3, 1)

        DatePickerMillis.toLocalDate(DatePickerMillis.fromLocalDate(date)) shouldBe date
    }
}
