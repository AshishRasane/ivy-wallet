package com.ivy.smstransactions.parser

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ivy.base.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import javax.inject.Inject

/**
 * Detects completed income/expense transactions in Indian bank SMS
 * (bank accounts, debit/credit cards, UPI and wallets). Money sent to the user's own
 * accounts elsewhere (credit card bills) is a transfer.
 *
 * Pure and stateless: it doesn't touch Android APIs, so it's fully unit-testable.
 */
class BankSmsParser @Inject constructor() {

    fun parse(sender: String, body: String): Either<SmsSkipReason, SmsTransaction> = either {
        val text = body.replace(WHITESPACE, " ").trim()

        ensure(isBankSender(sender)) { SmsSkipReason.NotFromBank }
        ensure(!OTP.containsMatchIn(text)) { SmsSkipReason.Otp }
        ensure(!FAILED.containsMatchIn(text)) { SmsSkipReason.FailedTransaction }
        ensure(!NOT_YET_HAPPENED.containsMatchIn(text)) { SmsSkipReason.NotYetHappened }
        ensure(!PROMOTIONAL.containsMatchIn(text)) { SmsSkipReason.Promotional }
        ensure(!isCreditCardBillPayment(text)) { SmsSkipReason.CreditCardBillPayment }
        ensure(BANKING_CONTEXT.containsMatchIn(text)) { SmsSkipReason.NotATransaction }

        val direction = ensureNotNull(detectType(text)) { SmsSkipReason.NotATransaction }
        val amount = ensureNotNull(detectAmount(text)) { SmsSkipReason.NotATransaction }

        SmsTransaction(
            type = if (direction == TransactionType.EXPENSE && isCardBillPayment(text)) {
                TransactionType.TRANSFER
            } else {
                direction
            },
            amount = amount,
            counterparty = detectCounterparty(text, direction),
            accountEnding = ACCOUNT_ENDING.find(text)?.groupValues?.get(1)?.takeLast(ACCOUNT_ENDING_DIGITS),
            bank = detectBank(sender, text),
            reference = detectReference(text),
            date = detectDate(text),
            time = detectTime(text),
        )
    }

    private fun isBankSender(sender: String): Boolean {
        val normalized = sender.trim().replace(" ", "")
        // Banks send from DLT headers (e.g. "AX-HDFCBK-S") or short codes,
        // never from personal 10-digit mobile numbers.
        if (PERSONAL_NUMBER.matches(normalized)) return false
        // TRAI "-P" suffix = promotional header
        return !PROMOTIONAL_HEADER.matches(normalized)
    }

    private fun isCreditCardBillPayment(text: String): Boolean =
        text.contains("credit card", ignoreCase = true) && CARD_PAYMENT_RECEIVED.containsMatchIn(text)

    /** Paying a credit card bill (e.g. via CRED) moves money to the card account; it isn't spending. */
    private fun isCardBillPayment(text: String): Boolean =
        CARD_BILL_PAYMENT.containsMatchIn(text) && !CARD_SPEND.containsMatchIn(text)

    private fun detectType(text: String): TransactionType? {
        // The first keyword wins: "A/c X debited ... & A/c Y credited" is a debit for the user.
        val debitAt = DEBIT.find(text)?.range?.first ?: Int.MAX_VALUE
        val creditAt = CREDIT.find(text)?.range?.first ?: Int.MAX_VALUE
        return when {
            debitAt == Int.MAX_VALUE && creditAt == Int.MAX_VALUE -> null
            debitAt < creditAt -> TransactionType.EXPENSE
            else -> TransactionType.INCOME
        }
    }

    private fun detectAmount(text: String): Double? {
        val currencyAmount = CURRENCY_AMOUNT.findAll(text)
            .firstOrNull { match ->
                // skip "Avl Bal Rs 1,000", "Avl Lmt INR 50,000", etc.
                val before = text.substring((match.range.first - LOOK_BEHIND_CHARS).coerceAtLeast(0), match.range.first)
                !BALANCE_CONTEXT.containsMatchIn(before)
            }
            ?.groupValues?.get(1)
        val raw = currencyAmount ?: BARE_AMOUNT.find(text)?.groupValues?.get(1)
        return raw?.replace(",", "")?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun detectCounterparty(text: String, type: TransactionType): String? {
        val patterns = if (type == TransactionType.EXPENSE) EXPENSE_COUNTERPARTY else INCOME_COUNTERPARTY
        return patterns.asSequence()
            .flatMap { it.findAll(text) }
            .mapNotNull { cleanCounterparty(it.groupValues[1]) }
            .firstOrNull()
    }

    private fun cleanCounterparty(raw: String): String? {
        val name = raw.trim().trimEnd('.', ',', '-', '*', ':', ';').replace(WHITESPACE, " ")
        return name.takeIf {
            it.length in 2..MAX_COUNTERPARTY_LENGTH &&
                !it.all(Char::isDigit) &&
                !NOT_A_COUNTERPARTY.containsMatchIn(it)
        }
    }

    private fun detectBank(sender: String, text: String): String? {
        // "AX-HDFCBK-S" -> "HDFCBK"
        val header = sender.uppercase().split("-").let { parts -> parts.getOrElse(1) { parts[0] } }
        return BANKS.firstOrNull { bank -> bank.senderCodes.any(header::contains) }?.name
            ?: BANKS.firstOrNull { bank -> bank.bodyPattern.containsMatchIn(text) }?.name
    }

    private fun detectReference(text: String): String? =
        (UPI_P2X_REFERENCE.find(text) ?: REFERENCE.find(text))?.groupValues?.get(1)

    private fun detectDate(text: String): LocalDate? =
        isoDate(text) ?: monthNameDate(text) ?: numericDate(text)

    private fun isoDate(text: String): LocalDate? = ISO_DATE.find(text)?.let { m ->
        val (y, mo, d) = m.destructured
        safeDate(y.toInt(), mo.toInt(), d.toInt())
    }

    private fun monthNameDate(text: String): LocalDate? = MONTH_NAME_DATE.find(text)?.let { m ->
        val (d, mon, y) = m.destructured
        val month = MONTHS.indexOf(mon.lowercase().take(MONTH_ABBREVIATION_LENGTH)) + 1
        safeDate(fullYear(y), month, d.toInt())
    }

    private fun numericDate(text: String): LocalDate? = NUMERIC_DATE.find(text)?.let { m ->
        val (d, mo, y) = m.destructured
        safeDate(fullYear(y), mo.toInt(), d.toInt())
    }

    private fun detectTime(text: String): LocalTime? {
        val groups = TIME.find(text)?.groupValues ?: return null
        var hour = groups[1].toInt()
        when (groups[TIME_AM_PM_GROUP].lowercase()) {
            "pm" -> if (hour < NOON_HOUR) hour += NOON_HOUR
            "am" -> if (hour == NOON_HOUR) hour = 0
        }
        val minute = groups[2].toInt()
        val second = groups[TIME_SECONDS_GROUP].toIntOrNull() ?: 0
        return runCatching { LocalTime.of(hour, minute, second) }.getOrNull()
    }

    private fun fullYear(year: String): Int = if (year.length == 2) CENTURY + year.toInt() else year.toInt()

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, Month.of(month), day) }.getOrNull()

    private class Bank(val name: String, val senderCodes: List<String>, val bodyPattern: Regex)

    companion object {
        private const val LOOK_BEHIND_CHARS = 20
        private const val MAX_COUNTERPARTY_LENGTH = 40
        private const val NOON_HOUR = 12
        private const val CENTURY = 2000
        private const val ACCOUNT_ENDING_DIGITS = 4
        private const val MONTH_ABBREVIATION_LENGTH = 3
        private const val TIME_SECONDS_GROUP = 3
        private const val TIME_AM_PM_GROUP = 4

        private val OPTS = setOf(RegexOption.IGNORE_CASE)
        private fun regex(pattern: String): Regex = Regex(pattern, OPTS)

        private val WHITESPACE = Regex("\\s+")
        private val PERSONAL_NUMBER = Regex("^(\\+?91)?[6-9]\\d{9}$")
        private val PROMOTIONAL_HEADER = regex("^[a-z]{2}-[a-z0-9]{3,9}-p$")

        private val OTP = regex("\\b(otp|one[- ]time password|verification code)\\b")
        private val FAILED = regex(
            "\\b(failed|declined|unsuccessful|rejected|could not be (processed|completed)|" +
                "has not been processed)\\b"
        )
        private val NOT_YET_HAPPENED = regex(
            "will be (debited|credited|deducted|charged)|to be (debited|deducted)|\\bis due\\b|" +
                "\\bdue (on|by|date)\\b|payment due|minimum (amount )?due|total (amount )?due|" +
                "\\bhas requested\\b|requested (money|payment)|collect request|" +
                "mandate (is |has been )?(created|registered|set ?up)|\\bscheduled (on|for)\\b|\\breminder\\b"
        )
        private val PROMOTIONAL = regex(
            "pre-?approved|apply now|\\beligible for\\b|limited period|\\bget (up ?to|flat)\\b|" +
                "\\bt&c\\b|\\btnc\\b"
        )
        private val CARD_PAYMENT_RECEIVED = regex(
            "\\bpayment\\b.*\\breceived\\b|\\breceived\\b.*\\bpayment\\b"
        )

        // "to CRED CCBP" (CRED's bill payments), "cred.club@axisb", "towards your credit card"
        private val CARD_BILL_PAYMENT = regex(
            "\\bccbp\\b|\\bcred\\.club\\b|credit ?card bill|towards (?:your )?(?:[a-z]+ )?credit ?card|" +
                "\\bcc (?:bill )?payment\\b"
        )

        // a purchase made *with* a card (e.g. rent paid on CRED with a credit card) is spending
        private val CARD_SPEND = regex("\\bspent\\b|\\bcard (?:no\\.? )?x*\\d+ (?:has been )?used\\b")
        private val BANKING_CONTEXT = regex(
            "(\\ba/c|\\bac\\b|\\bacct\\b|\\baccount\\b|\\bcard\\b|\\bupi\\b|\\bvpa\\b|\\bbank\\b|" +
                "\\bwallet\\b|\\bimps\\b|\\bneft\\b|\\brtgs\\b|\\batm\\b)"
        )

        private val DEBIT = regex(
            "\\b(debited|debit of|spent|paid|sent|withdrawn|withdrawal|purchase|deducted|" +
                "transferred to|used for|for using)\\b"
        )
        private val CREDIT = regex("\\b(credited|received|deposited|refund|refunded|reversed|cashback of)\\b")

        private const val NUMBER = "(\\d[\\d,]*(?:\\.\\d{1,2})?)"
        private val CURRENCY_AMOUNT = regex("(?:(?<![a-z])(?:rs|inr)\\.?|₹)\\s*[:.]?\\s*$NUMBER")

        // SBI UPI: "A/C X1234 debited by 150.0 on date 05May24"
        private val BARE_AMOUNT = regex("\\b(?:debited|credited)\\s+(?:by|for|with)\\s+$NUMBER")
        private val BALANCE_CONTEXT = regex("(bal|avl|avbl|avail|limit|lmt|due)")

        private val ACCOUNT_ENDING = regex(
            "(?:\\ba/c|\\bac\\b|\\bacct\\b|\\baccount\\b|\\bcard\\b)\\.?\\s*(?:no\\.?|number)?\\s*" +
                "(?:ending(?:\\s+(?:with|in))?)?\\s*[:\\-]?\\s*(?:\\d{0,4}[x*.]+)?(\\d{3,18})\\b"
        )

        private const val VPA = "([a-z0-9._\\-]+@[a-z][a-z0-9]+)"
        private const val NAME = "([a-z0-9][a-z0-9 &'._\\-]*?)"
        private const val NAME_END =
            "(?=\\s+on\\b|\\s+in\\b|\\s+via\\b|\\s+using\\b|\\s+thru\\b|\\s+ref|\\s+upi\\b|\\s+has\\b|" +
                "\\s+is\\b|\\s+was\\b|\\s+avl|\\s+avbl|\\s+not\\b|\\(|\\.\\s|\\.$|,|;|$)"
        private val UPI_P2X_NAME = regex("upi/p2[am]/\\d+/([^/]+?)(?=/|\\s+not\\b|\\s+if\\b|\\.\\s|\\.$|$)")

        // ICICI cards: "... debited for INR 448.00 on 09-Oct-26 for UPI-664800000001-CHAI POINT."
        private val UPI_DASH_NAME = regex("\\bupi-\\d{6,}-$NAME(?=\\.\\s|\\.$|,|;|\\s+on\\b|\\s+to\\b|$)")

        private val EXPENSE_COUNTERPARTY = listOf(
            UPI_P2X_NAME,
            UPI_DASH_NAME,
            // ICICI: "debited for Rs 240.00 on 05-May-24; SWIGGY credited."
            regex(";\\s*([^;.]+?)\\s+credited"),
            regex("\\b(?:to|at)\\s+(?:vpa\\s+)?$VPA"),
            regex("\\bat\\s+$NAME$NAME_END"),
            regex("\\b(?:to|towards)\\s+$NAME$NAME_END"),
        )
        private val INCOME_COUNTERPARTY = listOf(
            UPI_P2X_NAME,
            UPI_DASH_NAME,
            regex("\\b(?:from|by)\\s+(?:vpa\\s+)?$VPA"),
            regex(
                "\\b(?:from|by)\\s+(?:(?:transfer|trf|neft|imps|rtgs|upi)\\s+(?:from\\s+)?)?$NAME$NAME_END"
            ),
        )
        private val NOT_A_COUNTERPARTY = regex(
            "^(your|you|a/c|ac|acct|account|the|mobile|bank|card|upi|imps|neft|rtgs|transfer|trf|" +
                "date|self)\\b|^\\d{5,}|a/c|\\bacct\\b|\\baccount\\b|\\bcard\\b|\\bbank\\b|\\bblock\\b|linked"
        )

        private val UPI_P2X_REFERENCE = regex("upi/p2[am]/(\\d{6,})")
        private val REFERENCE = regex(
            "\\b(?:upi\\s*ref|ref|rrn|utr|imps|neft|txn|upi)(?:erence)?\\s*(?:no|number|id)?\\b" +
                "\\.?\\s*[:.#\\-/]?\\s*(\\d{6,})"
        )

        private val MONTHS = listOf(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
        )
        private val ISO_DATE = regex("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b")
        private val MONTH_NAME_DATE = regex(
            "\\b(\\d{1,2})[- ]?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[- ,]*(\\d{4}|\\d{2})\\b"
        )
        private val NUMERIC_DATE = regex("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4}|\\d{2})\\b")
        private val TIME = regex("(?<![\\d-])\\b(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?:\\s*(am|pm))?\\b")

        private val BANKS = listOf(
            Bank("HDFC Bank", listOf("HDFC"), regex("\\bhdfc\\b")),
            Bank("SBI", listOf("SBI"), regex("\\bsbi\\b|state bank")),
            Bank("ICICI Bank", listOf("ICICI"), regex("\\bicici\\b")),
            Bank("Axis Bank", listOf("AXIS"), regex("\\baxis\\b")),
            Bank("Kotak Bank", listOf("KOTAK"), regex("\\bkotak\\b")),
            Bank("Union Bank", listOf("UNION", "UBOI"), regex("union bank")),
            Bank("Bank of Baroda", listOf("BOB"), regex("bank of baroda|\\bbob\\b")),
            Bank("PNB", listOf("PNB"), regex("\\bpnb\\b|punjab national")),
            Bank("Canara Bank", listOf("CANBNK", "CANARA"), regex("\\bcanara\\b")),
            Bank("IDFC FIRST Bank", listOf("IDFC"), regex("\\bidfc\\b")),
            Bank("Yes Bank", listOf("YESBNK", "YESBK"), regex("\\byes bank\\b")),
            Bank("IndusInd Bank", listOf("INDUS"), regex("\\bindusind\\b")),
            Bank("Federal Bank", listOf("FEDBNK", "FEDERAL"), regex("federal bank")),
            Bank("Indian Bank", listOf("INDBNK"), regex("\\bindian bank\\b")),
            Bank("Bank of India", listOf("BOIIND"), regex("bank of india")),
            Bank("Paytm Payments Bank", listOf("PAYTM"), regex("\\bpaytm\\b")),
            Bank("slice", listOf("SLICE", "SLCEIT"), regex("\\bslice\\b")),
        )
    }
}
