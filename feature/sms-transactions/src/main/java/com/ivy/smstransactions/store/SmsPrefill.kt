package com.ivy.smstransactions.store

import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.domain.TransactionPrefill

/** The note saved with a transaction added from an SMS, e.g. "Added from SMS · HDFC Bank · A/c XX1234 · Ref 4123…". */
fun SmsTransactionEntity.prefillDescription(): String = listOfNotNull(
    "Added from SMS",
    bank,
    accountEnding?.let { "A/c XX$it" },
    reference?.let { "Ref $it" },
).joinToString(" · ")

fun SmsTransactionEntity.toPrefill(suggestion: SmsSuggestion): TransactionPrefill = TransactionPrefill(
    amount = amount,
    title = counterparty,
    description = prefillDescription(),
    dateTime = dateTime,
    accountId = suggestion.accountId,
    categoryId = suggestion.categoryId,
    toAccountId = suggestion.toAccountId,
    smsTransactionId = id,
)
