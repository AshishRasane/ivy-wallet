package com.ivy.wallet.domain.deprecated.logic

import com.ivy.data.model.IntervalType
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PlannedAutoAddTest {

    @Test
    fun `a daily SIP is due every day from its start up to now`() {
        // When
        val dates = PlannedAutoAdd.dueDates(START, 1, IntervalType.DAY, after = null, now = START.plusDays(3))

        // Then
        dates shouldBe listOf(START, START.plusDays(1), START.plusDays(2), START.plusDays(3))
    }

    @Test
    fun `only dates after the last recorded one are added`() {
        // Given - the first two days were recorded on an earlier run (and maybe one was deleted since)
        val after = START.plusDays(1)

        // When
        val dates = PlannedAutoAdd.dueDates(START, 1, IntervalType.DAY, after = after, now = START.plusDays(3))

        // Then
        dates shouldBe listOf(START.plusDays(2), START.plusDays(3))
    }

    @Test
    fun `a weekly SIP is due every 7 days and nothing is added before its start`() {
        PlannedAutoAdd.dueDates(START, 1, IntervalType.WEEK, after = null, now = START.plusDays(15)) shouldBe
            listOf(START, START.plusDays(7), START.plusDays(14))
        PlannedAutoAdd.dueDates(START, 1, IntervalType.WEEK, after = null, now = START.minusDays(1)) shouldBe
            emptyList()
    }

    @Test
    fun `a start far in the past is capped`() {
        val dates = PlannedAutoAdd.dueDates(START, 1, IntervalType.DAY, after = null, now = START.plusDays(5000))

        dates.size shouldBe PlannedAutoAdd.MAX_PER_RUN
    }

    private fun Instant.plusDays(days: Long): Instant = plus(days, ChronoUnit.DAYS)
    private fun Instant.minusDays(days: Long): Instant = minus(days, ChronoUnit.DAYS)

    companion object {
        private val START = Instant.parse("2026-10-01T03:30:00Z")
    }
}
