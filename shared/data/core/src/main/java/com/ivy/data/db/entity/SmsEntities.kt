package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ivy.base.model.TransactionType
import java.time.Instant
import java.util.UUID

/**
 * A transaction detected in a bank SMS. Only the parsed details are stored, never the SMS text.
 */
@Keep
@Entity(
    tableName = "sms_transactions",
    indices = [Index(value = ["fingerprint"], unique = true), Index(value = ["status"])]
)
data class SmsTransactionEntity(
    @PrimaryKey
    val id: UUID,
    /** Identifies the same bank transaction across duplicate SMS (UPI ref, else a content hash). */
    val fingerprint: String,
    val reference: String?,
    val type: TransactionType,
    val amount: Double,
    val counterparty: String?,
    val accountEnding: String?,
    val bank: String?,
    val dateTime: Instant,
    /** One of [SmsTransactionStatus]. */
    val status: String,
    val createdAt: Instant,
)

object SmsTransactionStatus {
    const val PENDING = "PENDING"
    const val ADDED = "ADDED"
    const val IGNORED = "IGNORED"
}

/** Learned: which Ivy account an SMS account (bank + last digits) belongs to. */
@Keep
@Entity(tableName = "sms_account_links")
data class SmsAccountLinkEntity(
    /** "<bank>|<accountEnding>" */
    @PrimaryKey
    val key: String,
    val accountId: UUID,
    val updatedAt: Instant,
)

/** Learned: which category a merchant's transactions go to. */
@Keep
@Entity(tableName = "sms_category_links")
data class SmsCategoryLinkEntity(
    /** Normalized merchant name, e.g. "swiggy". */
    @PrimaryKey
    val merchantKey: String,
    val categoryId: UUID,
    val updatedAt: Instant,
)
