package com.ivy.legacy.datamodel

import androidx.compose.runtime.Immutable
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.model.IntervalType
import java.time.Instant
import java.util.UUID

@Suppress("DataClassDefaultValues", "DataClassTypedIDs") // legacy model: optional fields, UUID ids
@Deprecated("Legacy data model. Will be deleted")
@Immutable
data class PlannedPaymentRule(
    val startDate: Instant?,
    val intervalN: Int?,
    val intervalType: IntervalType?,
    val oneTime: Boolean,

    val type: TransactionType,
    val accountId: UUID,
    val amount: Double = 0.0,
    val categoryId: UUID? = null,
    val title: String? = null,
    val description: String? = null,
    /** For a planned transfer: the account the money goes to. */
    val toAccountId: UUID? = null,
    /** Recorded automatically on each due date. */
    val autoAdd: Boolean = false,

    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,

    val id: UUID = UUID.randomUUID()
) {
    fun toEntity(): PlannedPaymentRuleEntity = PlannedPaymentRuleEntity(
        startDate = startDate,
        intervalN = intervalN,
        intervalType = intervalType,
        oneTime = oneTime,
        type = type,
        accountId = accountId,
        amount = amount,
        categoryId = categoryId,
        title = title,
        description = description,
        toAccountId = toAccountId,
        autoAdd = autoAdd,
        isSynced = isSynced,
        isDeleted = isDeleted,
        id = id
    )
}
