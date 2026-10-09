package com.ivy.smstransactions.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.smstransactions.SmsTransactionNotifier
import com.ivy.smstransactions.store.SmsSuggestion
import com.ivy.smstransactions.store.SmsTransactionStore
import com.ivy.smstransactions.store.toPrefill
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@Immutable
data class SmsReviewState(
    val items: ImmutableList<SmsReviewItem>,
)

/** Id of a stored SMS transaction (see SmsTransactionEntity). */
@JvmInline
value class SmsItemId(val value: UUID)

// SmsItemId is a typed value-class id; the detekt rule doesn't recognize it.
@Suppress("DataClassTypedIDs")
@Immutable
data class SmsReviewItem(
    val id: SmsItemId,
    val title: String,
    val amount: String,
    val isIncome: Boolean,
    /** e.g. "Today, 7:42 PM · HDFC Bank · A/c XX1234" */
    val details: String,
    /** e.g. "Bank · Food & Drinks", null until the account/category are learned. */
    val suggestion: String?,
)

sealed interface SmsReviewEvent {
    @Suppress("DataClassTypedIDs")
    data class Add(val id: SmsItemId) : SmsReviewEvent

    @Suppress("DataClassTypedIDs")
    data class Ignore(val id: SmsItemId) : SmsReviewEvent
}

@Stable
@HiltViewModel
class SmsReviewViewModel @Inject constructor(
    private val store: SmsTransactionStore,
    private val notifier: SmsTransactionNotifier,
    private val nav: Navigation,
    private val timeProvider: TimeProvider,
) : ComposeViewModel<SmsReviewState, SmsReviewEvent>() {

    private val pending = store.pending.map { list ->
        list.map { it to store.suggestion(it) }
    }

    @Composable
    override fun uiState(): SmsReviewState {
        val items = remember { pending }.collectAsState(initial = emptyList()).value
        return SmsReviewState(
            items = items.map { (trn, suggestion) -> trn.toItem(suggestion) }.toImmutableList()
        )
    }

    override fun onEvent(event: SmsReviewEvent) {
        when (event) {
            is SmsReviewEvent.Add -> add(event.id.value)
            is SmsReviewEvent.Ignore -> ignore(event.id.value)
        }
    }

    private fun add(id: UUID) {
        viewModelScope.launch {
            val trn = store.findById(id) ?: return@launch
            val suggestion = store.suggestion(trn)
            val prefill = trn.toPrefill(suggestion)
            nav.navigateTo(
                EditTransactionScreen(
                    initialTransactionId = null,
                    type = suggestion.type,
                    accountId = prefill.accountId,
                    categoryId = prefill.categoryId,
                    toAccountId = prefill.toAccountId,
                    amount = prefill.amount,
                    title = prefill.title,
                    description = prefill.description,
                    dateTime = prefill.dateTime,
                    smsTransactionId = prefill.smsTransactionId,
                )
            )
        }
    }

    private fun ignore(id: UUID) {
        viewModelScope.launch {
            store.markIgnored(id)
            notifier.dismiss(id)
        }
    }

    private fun SmsTransactionEntity.toItem(suggestion: SmsSuggestion): SmsReviewItem {
        val isIncome = type == TransactionType.INCOME
        val isTransfer = suggestion.type == TransactionType.TRANSFER
        val sign = if (isIncome) "+" else "−"
        return SmsReviewItem(
            id = SmsItemId(id),
            title = counterparty ?: when {
                isIncome -> "Income"
                isTransfer -> "Transfer"
                else -> "Expense"
            },
            amount = sign + SmsTransactionNotifier.formatInr(amount),
            isIncome = isIncome,
            details = listOfNotNull(formatTime(dateTime), bank, accountEnding?.let { "A/c XX$it" })
                .joinToString(" · "),
            suggestion = if (isTransfer) {
                "Transfer · " + listOfNotNull(suggestion.accountName, suggestion.toAccountName ?: "choose account")
                    .joinToString(" → ")
            } else {
                listOfNotNull(suggestion.accountName, suggestion.categoryName)
                    .joinToString(" · ")
                    .ifBlank { null }
            },
        )
    }

    private fun formatTime(time: Instant): String {
        val local = time.atZone(timeProvider.getZoneId()).toLocalDateTime()
        val today = timeProvider.localDateNow()
        val day = when (local.toLocalDate()) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> local.format(DateTimeFormatter.ofPattern("d MMM"))
        }
        return "$day, ${local.format(DateTimeFormatter.ofPattern("h:mm a"))}"
    }
}
