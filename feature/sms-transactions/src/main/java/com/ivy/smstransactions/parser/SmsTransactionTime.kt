package com.ivy.smstransactions.parser

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

private const val MaxDaysInPast = 31L
private val defaultTime: LocalTime = LocalTime.NOON

/**
 * Picks when the transaction happened.
 *
 * Banks send the SMS within seconds of the transaction, so the SMS receive time is the most
 * accurate. We only use the date printed in the SMS when it's an earlier day (e.g. a delayed
 * NEFT credit). Dates in the future or more than a month back are treated as mis-parsed.
 */
fun SmsTransaction.resolveDateTime(receivedAt: Instant, zone: ZoneId): Instant {
    val receivedDate = receivedAt.atZone(zone).toLocalDate()
    val earlierDay = date?.takeIf {
        it.isBefore(receivedDate) && !it.isBefore(receivedDate.minusDays(MaxDaysInPast))
    }
    return earlierDay?.atTime(time ?: defaultTime)?.atZone(zone)?.toInstant() ?: receivedAt
}
