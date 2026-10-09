package com.ivy.design.revamp

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Formats money for the revamped UI.
 *
 * - INR: "₹1,24,560.40" (symbol first, Indian lakh/crore grouping)
 * - other currencies: "1,234.50 USD" (international grouping, code last)
 * - negatives use a real minus sign: "−₹386.00"; [signed] also adds "+" to positives.
 */
object AmountFormat {
    private const val INR = "INR"
    private const val MINUS = "−"
    private const val THOUSANDS = 3
    private const val INDIAN_GROUP = 2

    fun format(
        amount: Double,
        currency: String,
        signed: Boolean = false,
        showDecimals: Boolean = true,
    ): String {
        val scale = if (showDecimals) 2 else 0
        val absolute = BigDecimal.valueOf(kotlin.math.abs(amount)).setScale(scale, RoundingMode.HALF_UP)
        val parts = absolute.toPlainString().split(".")
        val isInr = currency.equals(INR, ignoreCase = true)
        val whole = if (isInr) groupIndian(parts[0]) else groupInternational(parts[0])
        val number = if (parts.size > 1) "$whole.${parts[1]}" else whole

        val sign = when {
            absolute.signum() == 0 -> ""
            amount < 0 -> MINUS
            signed -> "+"
            else -> ""
        }
        return if (isInr) "$sign₹$number" else "$sign$number ${currency.uppercase()}".trim()
    }

    /** "12456040" -> "1,24,56,040" */
    private fun groupIndian(digits: String): String {
        if (digits.length <= THOUSANDS) return digits
        val last3 = digits.takeLast(THOUSANDS)
        val rest = digits.dropLast(THOUSANDS)
        return rest.reversed().chunked(INDIAN_GROUP).joinToString(",").reversed() + "," + last3
    }

    /** "1234567" -> "1,234,567" */
    private fun groupInternational(digits: String): String =
        digits.reversed().chunked(THOUSANDS).joinToString(",").reversed()
}
