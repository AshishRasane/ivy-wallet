package com.ivy.smstransactions.store

import java.security.MessageDigest

/**
 * Keys used to recognize duplicates and to remember the user's account/category choices.
 */
object SmsKeys {
    private const val HASH_HEX_LENGTH = 32
    private val nonAlphanumeric = Regex("[^a-z0-9 ]")
    private val whitespace = Regex("\\s+")

    /**
     * The same bank transaction can arrive twice (e.g. a UPI app and the bank both send an SMS,
     * or a resend). The UPI/IMPS reference identifies it; without one, the exact text does.
     */
    fun fingerprint(reference: String?, sender: String, body: String): String =
        if (reference != null) {
            "ref:$reference"
        } else {
            "hash:" + sha256("${sender.trim().uppercase()}|${body.replace(whitespace, " ").trim()}")
                .take(HASH_HEX_LENGTH)
        }

    /** "HDFC Bank" + "1234" -> "hdfc bank|1234". Null when the SMS has no account ending. */
    fun accountKey(bank: String?, accountEnding: String?): String? =
        accountEnding?.let { "${bank.orEmpty().lowercase()}|$it" }

    /**
     * "SWIGGY", "Swiggy " -> "swiggy"; "zomato@paytm" -> "zomato".
     * Null when there is no usable merchant name.
     */
    fun merchantKey(counterparty: String?): String? {
        val name = counterparty?.substringBefore("@")?.lowercase() ?: return null
        return name.replace(nonAlphanumeric, " ").replace(whitespace, " ").trim().ifBlank { null }
    }

    /**
     * Where money sent to this merchant goes when it's a transfer (e.g. "CRED CCBP" -> the
     * credit card account), stored with the account links: "payee|cred ccbp".
     */
    fun payeeKey(counterparty: String?): String? = merchantKey(counterparty)?.let { "payee|$it" }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
