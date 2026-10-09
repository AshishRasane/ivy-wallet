package com.ivy.domain

import java.util.UUID

/**
 * Lets screens report back about transactions that were detected in a bank SMS,
 * without depending on the SMS feature module.
 */
interface SmsTransactionCallbacks {
    /**
     * The user saved the SMS transaction [smsTransactionId] with this account and category,
     * or as a transfer to [toAccountId].
     */
    suspend fun onSaved(smsTransactionId: UUID, accountId: UUID, categoryId: UUID?, toAccountId: UUID?)
}
