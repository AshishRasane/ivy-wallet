package com.ivy.transaction.revamp

import java.math.BigDecimal

/**
 * Keypad input for the amount. The text is what the user typed ("2499", "2499.", "2499.5");
 * the screen shows it formatted and sends the parsed amount to the view model.
 */
object AmountInput {
    const val DOT = "."
    const val DELETE = "⌫"
    private const val MAX_WHOLE_DIGITS = 12
    private const val INDIAN_GROUP = 2
    private const val THOUSANDS = 3

    fun press(text: String, key: String, maxDecimals: Int): String = when (key) {
        DELETE -> text.dropLast(1)
        DOT -> when {
            maxDecimals == 0 || text.contains(DOT) -> text
            text.isEmpty() -> "0$DOT"
            else -> text + DOT
        }
        else -> pressDigit(text, key, maxDecimals)
    }

    private fun pressDigit(text: String, digit: String, maxDecimals: Int): String {
        val dot = text.indexOf(DOT)
        return when {
            dot >= 0 && text.length - dot - 1 >= maxDecimals -> text
            dot < 0 && text.length >= MAX_WHOLE_DIGITS -> text
            text == "0" -> digit
            else -> text + digit
        }
    }

    fun toAmount(text: String): Double = text.trimEnd('.').toDoubleOrNull() ?: 0.0

    /** 2499.0 -> "2499", 2499.5 -> "2499.5", 0.0 -> "" */
    fun fromAmount(amount: Double): String {
        if (amount <= 0.0) return ""
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()
    }

    /**
     * Groups the whole part while typing: "1245604" -> "12,45,604" (INR) or "1,245,604";
     * a trailing "." or partial decimals are kept as typed.
     */
    fun display(text: String, indianGrouping: Boolean): String {
        if (text.isEmpty()) return "0"
        val whole = text.substringBefore(DOT)
        val rest = if (text.contains(DOT)) DOT + text.substringAfter(DOT) else ""
        val grouped = if (indianGrouping) groupIndian(whole) else groupInternational(whole)
        return grouped + rest
    }

    private fun groupIndian(digits: String): String {
        if (digits.length <= THOUSANDS) return digits
        val rest = digits.dropLast(THOUSANDS)
        return rest.reversed().chunked(INDIAN_GROUP).joinToString(",").reversed() + "," + digits.takeLast(THOUSANDS)
    }

    private fun groupInternational(digits: String): String =
        digits.reversed().chunked(THOUSANDS).joinToString(",").reversed()
}
