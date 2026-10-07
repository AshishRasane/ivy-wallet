package com.ivy.ui.time.impl

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material3 `DatePicker` represents a calendar day as the epoch millis of that day's
 * midnight in **UTC**, both for `initialSelectedDateMillis` and `selectedDateMillis`.
 * Converting those millis with the device's time zone shifts the date by one day
 * for users west of UTC (and for early-morning times east of UTC).
 */
internal object DatePickerMillis {
    fun fromLocalDate(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
