package com.ivy.piechart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.model.Category
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.FromToTimeRange
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.ioThread
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.piechart.action.PieChartAct
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

private const val TrendMonths = 6

@Stable
@HiltViewModel
class PieChartStatisticViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val ivyContext: IvyWalletCtx,
    private val pieChartAct: PieChartAct,
    private val sharedPrefs: SharedPrefs,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<PieChartStatisticState, PieChartStatisticEvent>() {

    private var treatTransfersAsIncomeExpense by mutableStateOf(false)
    private var transactionType by mutableStateOf(TransactionType.INCOME)
    private var period by mutableStateOf(TimePeriod())
    private var baseCurrency by mutableStateOf("")
    private var totalAmount by mutableDoubleStateOf(0.0)
    private var categoryAmounts by mutableStateOf<ImmutableList<CategoryAmount>>(persistentListOf())
    private var selectedCategory by mutableStateOf<SelectedCategory?>(null)
    private var accountIdFilterList by mutableStateOf<ImmutableList<UUID>>(persistentListOf())
    private var showCloseButtonOnly by mutableStateOf(false)
    private var filterExcluded by mutableStateOf(false)
    private var transactions by mutableStateOf<ImmutableList<Transaction>>(persistentListOf())
    private var choosePeriodModal by mutableStateOf<ChoosePeriodModalData?>(null)
    private var trend by mutableStateOf<ImmutableList<TrendBar>>(persistentListOf())
    private var trendAverage by mutableStateOf<String?>(null)
    private var comparison by mutableStateOf<String?>(null)
    private var transactionCount by mutableIntStateOf(0)

    @Composable
    override fun uiState(): PieChartStatisticState {
        return PieChartStatisticState(
            transactionType = getTransactionType(),
            period = getPeriod(),
            baseCurrency = getBaseCurrency(),
            totalAmount = getTotalAmount(),
            categoryAmounts = getCategoryAmounts(),
            selectedCategory = getSelectedCategory(),
            accountIdFilterList = getAccountIdFilterList(),
            showCloseButtonOnly = getShowCloseButtonOnly(),
            filterExcluded = getFilterExcluded(),
            transactions = getTransactions(),
            choosePeriodModal = getChoosePeriodModal(),
            trend = trend,
            trendAverage = trendAverage,
            comparison = comparison,
            transactionCount = transactionCount,
        )
    }

    @Composable
    private fun getTransactionType(): TransactionType {
        return transactionType
    }

    @Composable
    private fun getPeriod(): TimePeriod {
        return period
    }

    @Composable
    private fun getBaseCurrency(): String {
        return baseCurrency
    }

    @Composable
    private fun getTotalAmount(): Double {
        return totalAmount
    }

    @Composable
    private fun getCategoryAmounts(): ImmutableList<CategoryAmount> {
        return categoryAmounts
    }

    @Composable
    private fun getSelectedCategory(): SelectedCategory? {
        return selectedCategory
    }

    @Composable
    private fun getAccountIdFilterList(): ImmutableList<UUID> {
        return accountIdFilterList
    }

    @Composable
    private fun getShowCloseButtonOnly(): Boolean {
        return showCloseButtonOnly
    }

    @Composable
    private fun getFilterExcluded(): Boolean {
        return filterExcluded
    }

    @Composable
    private fun getTransactions(): ImmutableList<Transaction> {
        return transactions
    }

    @Composable
    private fun getChoosePeriodModal(): ChoosePeriodModalData? {
        return choosePeriodModal
    }

    override fun onEvent(event: PieChartStatisticEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is PieChartStatisticEvent.OnSelectNextMonth -> nextMonth()
                is PieChartStatisticEvent.OnSelectPreviousMonth -> previousMonth()
                is PieChartStatisticEvent.OnSetPeriod -> onSetPeriod(event.timePeriod)
                is PieChartStatisticEvent.OnShowMonthModal -> configureMonthModal(event.timePeriod)
                is PieChartStatisticEvent.OnCategoryClicked -> onCategoryClicked(event.category)
                is PieChartStatisticEvent.OnStart -> start(event.screen)
                is PieChartStatisticEvent.OnTypeChanged -> {
                    transactionType = event.type
                    load(periodValue = period)
                }
            }
        }
    }

    private fun start(
        screen: PieChartStatisticScreen
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            startInternally(
                period = ivyContext.selectedPeriod,
                type = screen.type,
                accountIdFilterList = screen.accountList,
                filterExclude = screen.filterExcluded,
                transactions = screen.transactions,
                transfersAsIncomeExpenseValue = screen.treatTransfersAsIncomeExpense
            )
        }
    }

    private suspend fun startInternally(
        period: TimePeriod,
        type: TransactionType,
        accountIdFilterList: ImmutableList<UUID>,
        filterExclude: Boolean,
        transactions: ImmutableList<Transaction>,
        transfersAsIncomeExpenseValue: Boolean
    ) {
        initialise(period, type, accountIdFilterList, filterExclude, transactions)
        treatTransfersAsIncomeExpense = transfersAsIncomeExpenseValue
        load(periodValue = period)
    }

    private suspend fun initialise(
        periodValue: TimePeriod,
        type: TransactionType,
        accountIdFilterListValue: ImmutableList<UUID>,
        filterExcludedValue: Boolean,
        transactionsValue: ImmutableList<Transaction>
    ) {
        val settings = ioThread { settingsDao.findFirst() }
        val baseCurrencyValue = settings.currency

        period = periodValue
        transactionType = type
        accountIdFilterList = accountIdFilterListValue
        filterExcluded = filterExcludedValue
        transactions = transactionsValue
        showCloseButtonOnly = transactionsValue.isNotEmpty()
        baseCurrency = baseCurrencyValue
    }

    private suspend fun load(
        periodValue: TimePeriod
    ) {
        val type = transactionType
        val accountIdFilterList = accountIdFilterList
        val transactions = transactions
        val baseCurrency = baseCurrency
        val range = periodValue.toRange(ivyContext.startDayOfMonth, timeConverter, timeProvider)

        val pieChartActOutput = pieChart(range, type)

        val totalAmountValue = pieChartActOutput.totalAmount
        val categoryAmountsValue = pieChartActOutput.categoryAmounts

        period = periodValue
        totalAmount = totalAmountValue
        categoryAmounts = categoryAmountsValue
        selectedCategory = null
        transactionCount = pieChartActOutput.transactionCount
        loadTrend(periodValue, type, totalAmountValue)
    }

    private suspend fun pieChart(range: FromToTimeRange, type: TransactionType): PieChartAct.Output {
        val treatTransferAsIncExp =
            sharedPrefs.getBoolean(
                SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE,
                false
            ) && accountIdFilterList.isNotEmpty() && treatTransfersAsIncomeExpense

        return ioThread {
            pieChartAct(
                PieChartAct.Input(
                    baseCurrency = baseCurrency,
                    range = range,
                    type = type,
                    accountIdFilterList = accountIdFilterList,
                    treatTransferAsIncExp = treatTransferAsIncExp,
                    existingTransactions = transactions,
                    showAccountTransfersCategory = accountIdFilterList.isNotEmpty()
                )
            )
        }
    }

    /** Totals of the last months for the trend chart and the comparison; only for a month period. */
    private suspend fun loadTrend(periodValue: TimePeriod, type: TransactionType, currentTotal: Double) {
        val month = periodValue.month
        if (month == null || transactions.isNotEmpty()) {
            trend = persistentListOf()
            trendAverage = null
            comparison = null
            return
        }
        val selected = YearMonth.of(periodValue.year ?: dateNowUTC().year, month.monthValue)
        val months = ReportMath.lastMonths(selected, TrendMonths)
        val totals = months.map { yearMonth ->
            if (yearMonth == selected) {
                currentTotal
            } else {
                val monthPeriod = TimePeriod(month = Month.fromMonthValue(yearMonth.monthValue), year = yearMonth.year)
                val range = monthPeriod.toRange(ivyContext.startDayOfMonth, timeConverter, timeProvider)
                pieChart(range, type).totalAmount
            }
        }
        val previous = totals.dropLast(1)
        trend = ReportMath.trend(months, totals, baseCurrency).toImmutableList()
        trendAverage = previous.filter { it > 0.0 }.takeIf { it.isNotEmpty() }
            ?.let { "${previous.size}-month avg ${ReportMath.short(it.average(), baseCurrency)}" }
        comparison = ReportMath.comparison(currentTotal, previous.last(), months[months.lastIndex - 1])
    }

    private suspend fun onSetPeriod(periodValue: TimePeriod) {
        ivyContext.updateSelectedPeriodInMemory(periodValue)
        load(
            periodValue = periodValue
        )
    }

    private suspend fun nextMonth() {
        val month = period.month
        val year = period.year ?: com.ivy.legacy.utils.dateNowUTC().year
        if (month != null) {
            load(
                periodValue = month.incrementMonthPeriod(ivyContext, 1L, year)
            )
        }
    }

    private suspend fun previousMonth() {
        val month = period.month
        val year = period.year ?: com.ivy.legacy.utils.dateNowUTC().year
        if (month != null) {
            load(
                periodValue = month.incrementMonthPeriod(ivyContext, -1L, year)
            )
        }
    }

    private suspend fun configureMonthModal(timePeriod: TimePeriod?) {
        val choosePeriodModalData = if (timePeriod != null) {
            ChoosePeriodModalData(period = timePeriod)
        } else {
            null
        }

        choosePeriodModal = choosePeriodModalData
    }

    private suspend fun onCategoryClicked(clickedCategory: Category?) {
        val selectedCategoryValue = if (clickedCategory == selectedCategory?.category) {
            null
        } else {
            clickedCategory?.let { SelectedCategory(category = it) }
        }

        val existingCategoryAmounts = categoryAmounts
        val newCategoryAmounts = if (selectedCategoryValue != null) {
            existingCategoryAmounts
                .sortedByDescending { it.amount }
                .sortedByDescending {
                    selectedCategoryValue.category == it.category
                }
        } else {
            existingCategoryAmounts.sortedByDescending {
                it.amount
            }
        }.toImmutableList()

        selectedCategory = selectedCategoryValue
        categoryAmounts = newCategoryAmounts
    }
}
