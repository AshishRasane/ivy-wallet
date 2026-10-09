package com.ivy.piechart

import androidx.compose.runtime.Immutable
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** One bar of the "Last 6 months" chart. */
@Immutable
data class TrendBar(
    /** "Oct" */
    val month: String,
    /** "₹28.4k" */
    val amount: String,
    /** 0..1, relative to the highest month */
    val fraction: Float,
    val current: Boolean,
)

/**
 * The numbers around the chart on the Reports screen.
 * Pure: no Android or database access, so it's unit-tested.
 */
object ReportMath {
    private const val PERCENT = 100
    private const val THOUSAND = 1_000.0
    private const val LAKH = 1_00_000.0
    private const val CRORE = 1_00_00_000.0
    private const val MILLION = 1_000_000.0

    /** The [count] months up to and including [last], oldest first. */
    fun lastMonths(last: YearMonth, count: Int): List<YearMonth> =
        (count - 1 downTo 0).map { last.minusMonths(it.toLong()) }

    /** "12% less than September", or null when there's nothing to compare with. */
    fun comparison(current: Double, previous: Double, previousMonth: YearMonth): String? {
        if (previous <= 0.0) return null
        val name = previousMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val percent = ((current - previous) / previous * PERCENT).roundToInt()
        return when {
            percent == 0 -> "Same as $name"
            percent < 0 -> "${-percent}% less than $name"
            else -> "$percent% more than $name"
        }
    }

    /** Bars for [totals] (one per month in [months], oldest first); the last one is the current month. */
    fun trend(months: List<YearMonth>, totals: List<Double>, currency: String): List<TrendBar> {
        val max = totals.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        return months.zip(totals).mapIndexed { index, (month, total) ->
            TrendBar(
                month = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                // bars are narrow: no currency code (the average next to the title has it)
                amount = short(total, currency, withCode = false),
                fraction = (total / max).toFloat(),
                current = index == months.lastIndex,
            )
        }
    }

    /** "₹31.2k", "₹1.2L", "₹2.5Cr" for INR; "31.2k USD", "1.2M USD" (or "31.2k" without [withCode]) otherwise. */
    fun short(amount: Double, currency: String, withCode: Boolean = true): String {
        val value = abs(amount)
        val isInr = currency.equals("INR", ignoreCase = true)
        val number = when {
            isInr && value >= CRORE -> one(value / CRORE) + "Cr"
            isInr && value >= LAKH -> one(value / LAKH) + "L"
            !isInr && value >= MILLION -> one(value / MILLION) + "M"
            value >= THOUSAND -> one(value / THOUSAND) + "k"
            else -> value.roundToInt().toString()
        }
        return when {
            isInr -> "₹$number"
            withCode -> "$number ${currency.uppercase()}"
            else -> number
        }
    }

    private fun one(value: Double): String = String.format(Locale.ENGLISH, "%.1f", value).removeSuffix(".0")
}
