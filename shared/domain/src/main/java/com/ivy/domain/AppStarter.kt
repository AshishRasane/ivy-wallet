package com.ivy.domain

import android.content.Intent
import com.ivy.base.model.TransactionType
import java.time.Instant
import java.util.UUID

/**
 * A component used to start the **RootActivity** without knowing about it.
 */
interface AppStarter {
    fun getRootIntent(): Intent
    fun defaultStart()
    fun addTransactionStart(type: TransactionType)

    /**
     * @return an intent that opens the "add transaction" screen
     * pre-filled with [prefill]. Suitable for a notification's PendingIntent.
     */
    fun getAddTransactionIntent(
        type: TransactionType,
        prefill: TransactionPrefill,
    ): Intent
}

@Suppress("DataClassDefaultValues", "DataClassTypedIDs") // optional fields; legacy UUID ids
data class TransactionPrefill(
    val amount: Double?,
    val title: String?,
    val description: String?,
    val dateTime: Instant?,
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    /** Set when the transaction comes from a bank SMS (see [SmsTransactionCallbacks]). */
    val smsTransactionId: UUID? = null,
)
