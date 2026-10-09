package com.ivy.piechart

import androidx.compose.runtime.Immutable
import com.ivy.data.model.AccountId

/** Net money moved into one investment account in the period (negative: more withdrawn). */
// AccountId is a typed value-class id; the detekt rule doesn't recognize it.
@Suppress("DataClassTypedIDs")
@Immutable
data class InvestedAccount(
    val accountId: AccountId,
    val name: String,
    /** ARGB */
    val color: Int,
    val amount: Double,
)
