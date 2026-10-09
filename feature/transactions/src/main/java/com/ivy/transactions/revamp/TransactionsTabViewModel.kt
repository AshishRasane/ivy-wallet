package com.ivy.transactions.revamp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Transaction
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.data.model.toUTCCloseTimeRange
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.SearchScreen
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TransactionsTabState(
    /** e.g. "October 2026" */
    val month: String,
    val filter: TransactionFilter,
    val groups: ImmutableList<TransactionDayGroupUi>,
    val loading: Boolean,
)

sealed interface TransactionsTabEvent {
    data class SelectFilter(val filter: TransactionFilter) : TransactionsTabEvent
    data object PreviousMonth : TransactionsTabEvent
    data object NextMonth : TransactionsTabEvent
    data object Search : TransactionsTabEvent
    data class Open(val row: TransactionRowUi) : TransactionsTabEvent
}

/**
 * The Transactions tab: all transactions of the selected month (shared with Home), grouped by day.
 */
@Stable
@HiltViewModel
class TransactionsTabViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val startDayOfMonthAct: StartDayOfMonthAct,
    private val ivyContext: IvyWalletCtx,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val nav: Navigation,
) : ComposeViewModel<TransactionsTabState, TransactionsTabEvent>() {

    private var period by mutableStateOf<TimePeriod?>(null)
    private var filter by mutableStateOf(TransactionFilter.ALL)
    private var loading by mutableStateOf(true)
    private var data by mutableStateOf(MonthData.Empty)

    private data class MonthData(
        val transactions: List<Transaction>,
        val accounts: Map<AccountId, Account>,
        val categories: Map<CategoryId, Category>,
        val baseCurrency: String,
    ) {
        companion object {
            val Empty = MonthData(emptyList(), emptyMap(), emptyMap(), "")
        }
    }

    @Composable
    override fun uiState(): TransactionsTabState {
        LaunchedEffect(Unit) {
            load(initial = true)
        }
        val groups = remember(data, filter) {
            TransactionRows.group(
                transactions = data.transactions,
                accounts = data.accounts,
                categories = data.categories,
                baseCurrency = data.baseCurrency,
                zone = timeProvider.getZoneId(),
                today = timeProvider.localDateNow(),
                filter = filter,
            )
        }
        return TransactionsTabState(
            month = period?.label().orEmpty(),
            filter = filter,
            groups = if (loading && data.transactions.isEmpty()) persistentListOf() else groups,
            loading = loading,
        )
    }

    override fun onEvent(event: TransactionsTabEvent) {
        when (event) {
            is TransactionsTabEvent.SelectFilter -> filter = event.filter
            TransactionsTabEvent.PreviousMonth -> changeMonth(-1)
            TransactionsTabEvent.NextMonth -> changeMonth(1)
            TransactionsTabEvent.Search -> nav.navigateTo(SearchScreen)
            is TransactionsTabEvent.Open -> nav.navigateTo(
                EditTransactionScreen(initialTransactionId = event.row.id, type = event.row.type)
            )
        }
    }

    private suspend fun load(initial: Boolean = false) {
        loading = true
        if (initial) {
            ivyContext.initSelectedPeriodInMemory(startDayOfMonth = startDayOfMonthAct(Unit))
        }
        val selected = ivyContext.selectedPeriod
        val range = selected.toRange(
            startDateOfMonth = ivyContext.startDayOfMonth,
            timeConverter = timeConverter,
            timeProvider = timeProvider,
        ).toUTCCloseTimeRange()

        data = MonthData(
            transactions = transactionRepository.findAllBetween(range.from, range.to),
            accounts = accountRepository.findAll().associateBy { it.id },
            categories = categoryRepository.findAll().associateBy { it.id },
            baseCurrency = currencyRepository.getBaseCurrency().code,
        )
        period = selected
        loading = false
    }

    private fun changeMonth(increment: Long) {
        val current = period ?: return
        val month = current.month ?: return
        val next = month.incrementMonthPeriod(ivyContext, increment, year = current.year ?: dateNowUTC().year)
        ivyContext.updateSelectedPeriodInMemory(next)
        viewModelScope.launch { load() }
    }

    private fun TimePeriod.label(): String {
        val monthName = month?.name ?: return "Custom period"
        return listOfNotNull(monthName, year?.toString()).joinToString(" ")
    }
}
