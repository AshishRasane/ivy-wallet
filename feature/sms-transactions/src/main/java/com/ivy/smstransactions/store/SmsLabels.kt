package com.ivy.smstransactions.store

/**
 * How the SMS's account is shown: "A/c XX1234", or the card's name when the SMS has no
 * digits ("Scapia Federal Visa card").
 */
fun accountLabel(accountEnding: String?): String? = accountEnding?.let { ending ->
    if (ending.all(Char::isDigit)) "A/c XX$ending" else "$ending card"
}
