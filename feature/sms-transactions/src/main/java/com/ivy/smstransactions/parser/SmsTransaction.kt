package com.ivy.smstransactions.parser

import com.ivy.base.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime

/**
 * A transaction detected in a bank SMS.
 */
data class SmsTransaction(
    /**
     * [TransactionType.INCOME], [TransactionType.EXPENSE], or [TransactionType.TRANSFER] for money
     * sent to the user's own account elsewhere (a credit card bill, an investment app).
     * Money coming back from an investment app stays INCOME here; the store suggests it as a transfer.
     */
    val type: TransactionType,
    val amount: Double,
    /** The other side of the transaction, e.g. "SWIGGY" or "name@okaxis". */
    val counterparty: String?,
    /** The last digits of the account/card, e.g. "1234". */
    val accountEnding: String?,
    val bank: String?,
    /** UPI RRN / IMPS / NEFT reference number. */
    val reference: String?,
    val date: LocalDate?,
    val time: LocalTime?,
)

/**
 * Why an SMS wasn't recognized as a completed income/expense transaction.
 */
sealed interface SmsSkipReason {
    /** E.g. a personal mobile number or a promotional sender. */
    data object NotFromBank : SmsSkipReason
    data object Otp : SmsSkipReason

    /** "will be debited", bill due, collect requests, mandate set-up, etc. */
    data object NotYetHappened : SmsSkipReason
    data object FailedTransaction : SmsSkipReason
    data object Promotional : SmsSkipReason

    /** Card-side "payment received" SMS; the bank-side debit is the real expense. */
    data object CreditCardBillPayment : SmsSkipReason

    /** No amount, no debit/credit keyword or no banking context. */
    data object NotATransaction : SmsSkipReason
}
