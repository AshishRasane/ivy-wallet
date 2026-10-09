package com.ivy.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.revamp.AmountFormat
import com.ivy.design.revamp.RevampCircleButton
import com.ivy.design.revamp.RevampTopBar
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ui.component.transaction.dueSections
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.navigation
import com.ivy.transactions.revamp.TransactionDayGroup
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.Purple1Dark
import com.ivy.wallet.ui.theme.Red3Light
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

private val ScreenPadding = 20.dp
private const val ScrimAlpha = 0.85f

@Composable
fun BoxWithConstraintsScope.ReportScreen(
    @Suppress("UnusedParameter") screen: ReportScreen
) {
    val viewModel: ReportViewModel = viewModel()
    val state = viewModel.uiState()

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@Suppress("LongMethod")
@Composable
private fun BoxWithConstraintsScope.UI(
    state: ReportScreenState = ReportScreenState(),
    onEventHandler: (ReportScreenEvent) -> Unit = {}
) {
    val nav = navigation()
    val context = LocalContext.current
    val colors = revampColors()
    val listState = rememberScrollPositionListState(key = "reports")
    val openPieChart = { type: TransactionType ->
        if (state.transactions.isNotEmpty()) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = type,
                    transactions = state.transactions.toImmutableList(),
                    accountList = state.accountIdFilters,
                    treatTransfersAsIncomeExpense = state.treatTransfersAsIncExp
                )
            )
        }
    }
    val showFilter = { onEventHandler(ReportScreenEvent.OnFilterOverlayVisible(filterOverlayVisible = true)) }

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
                .background(colors.ground.copy(alpha = ScrimAlpha))
                .clickableNoIndication(rememberInteractionSource()) {
                    // consume clicks
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(R.string.generating_report), style = RevampType.title, color = colors.ink)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .systemBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RevampTopBar(title = "Custom report", onBack = { nav.back() }) {
                if (state.filter != null) {
                    RevampCircleButton(
                        icon = Icons.Filled.IosShare,
                        contentDescription = "Export to CSV",
                        onClick = { onEventHandler(ReportScreenEvent.OnExport(context = context)) },
                    )
                }
                RevampCircleButton(icon = Icons.Filled.FilterAlt, contentDescription = "Filter", onClick = showFilter)
            }
        }

        if (state.filter == null) {
            item { NoFilter(onSetFilter = showFilter) }
        } else {
            item {
                ReportSummary(
                    net = AmountFormat.format(state.balance, state.baseCurrency, signed = true),
                    netColor = when {
                        state.balance > 0 -> colors.income
                        state.balance < 0 -> colors.expense
                        else -> colors.ink
                    },
                    income = AmountFormat.format(state.income, state.baseCurrency),
                    expenses = AmountFormat.format(state.expenses, state.baseCurrency),
                    onIncomeClick = { openPieChart(TransactionType.INCOME) },
                    onExpensesClick = { openPieChart(TransactionType.EXPENSE) },
                    transfersAsIncomeExpense = state.treatTransfersAsIncExp
                        .takeIf { state.showTransfersAsIncExpCheckbox },
                    onTransfersAsIncomeExpense = {
                        onEventHandler(ReportScreenEvent.OnTreatTransfersAsIncomeExpense(transfersAsIncomeExpense = it))
                    },
                )
            }

            dueSections(
                baseData = AppBaseData(
                    baseCurrency = state.baseCurrency,
                    categories = state.categories,
                    accounts = state.accounts,
                ),
                upcoming = LegacyDueSection(
                    trns = state.upcomingTransactions,
                    stats = IncomeExpensePair(
                        income = state.upcomingIncome.toBigDecimal(),
                        expense = state.upcomingExpenses.toBigDecimal()
                    ),
                    expanded = state.upcomingExpanded
                ),
                overdue = LegacyDueSection(
                    trns = state.overdueTransactions,
                    stats = IncomeExpensePair(
                        income = state.overdueIncome.toBigDecimal(),
                        expense = state.overdueExpenses.toBigDecimal()
                    ),
                    expanded = state.overdueExpanded
                ),
                shouldShowAccountSpecificColorInTransactions = state.showAccountColorsInTransactions,
                onPayOrGet = { onEventHandler(ReportScreenEvent.OnPayOrGetLegacy(transaction = it)) },
                setUpcomingExpanded = { onEventHandler(ReportScreenEvent.OnUpcomingExpanded(upcomingExpanded = it)) },
                setOverdueExpanded = { onEventHandler(ReportScreenEvent.OnOverdueExpanded(overdueExpanded = it)) },
                onSkipTransaction = { onEventHandler(ReportScreenEvent.SkipTransactionLegacy(transaction = it)) },
                onSkipAllTransactions = { onEventHandler(ReportScreenEvent.SkipTransactionsLegacy(transactions = it)) },
            )

            if (state.historyGroups.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPadding, vertical = 32.dp),
                        text = stringResource(R.string.no_transactions_for_your_filter),
                        style = RevampType.body,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(state.historyGroups, key = { "day-${it.label}" }) { group ->
                TransactionDayGroup(
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                    group = group,
                    onTransactionClick = { row ->
                        nav.navigateTo(EditTransactionScreen(initialTransactionId = row.id, type = row.type))
                    },
                )
            }
        }
    }

    FilterOverlay(
        visible = state.filterOverlayVisible,
        baseCurrency = state.baseCurrency,
        accounts = state.accounts,
        categories = state.categories,
        filter = state.filter,
        allTags = state.allTags,
        onClose = {
            onEventHandler.invoke(
                ReportScreenEvent.OnFilterOverlayVisible(
                    filterOverlayVisible = false
                )
            )
        },
        onSetFilter = {
            onEventHandler.invoke(ReportScreenEvent.OnFilter(filter = it))
        },
        onTagSearch = {
            onEventHandler.invoke(ReportScreenEvent.OnTagSearch(data = it))
        }
    )
}

@Composable
private fun NoFilter(onSetFilter: () -> Unit) {
    val colors = revampColors()
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primaryTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.FilterAlt, contentDescription = null, tint = colors.onPrimaryTint)
        }
        Text(text = "Choose what to include", style = RevampType.title, color = colors.ink)
        Text(
            text = "Pick accounts, categories, tags, dates or amounts. You can export the result to CSV.",
            style = RevampType.label,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(min = 48.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable(role = Role.Button, onClick = onSetFilter)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.set_filter), style = RevampType.bodyStrong, color = colors.onPrimary)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ReportSummary(
    net: String,
    netColor: Color,
    income: String,
    expenses: String,
    onIncomeClick: () -> Unit,
    onExpensesClick: () -> Unit,
    /** null hides the switch */
    transfersAsIncomeExpense: Boolean?,
    onTransfersAsIncomeExpense: (Boolean) -> Unit,
) {
    val colors = revampColors()
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text(text = "Net", style = RevampType.label, color = colors.inkMuted)
            Text(text = net, style = RevampType.display, color = netColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowTile(
                modifier = Modifier.weight(1f),
                label = "Income",
                amount = income,
                amountColor = colors.income,
                tint = colors.incomeTint,
                onClick = onIncomeClick,
            )
            FlowTile(
                modifier = Modifier.weight(1f),
                label = "Expenses",
                amount = expenses,
                amountColor = colors.expense,
                tint = colors.expenseTint,
                onClick = onExpensesClick,
            )
        }
        if (transfersAsIncomeExpense != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Switch) { onTransfersAsIncomeExpense(!transfersAsIncomeExpense) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.transfers_as_income_expense),
                    style = RevampType.body,
                    color = colors.ink,
                )
                Switch(
                    checked = transfersAsIncomeExpense,
                    onCheckedChange = onTransfersAsIncomeExpense,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.inkMuted,
                        uncheckedTrackColor = colors.ground,
                        uncheckedBorderColor = colors.border,
                    ),
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun FlowTile(
    label: String,
    amount: String,
    amountColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint)
            .clickable(role = Role.Button, onClickLabel = "By category", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = RevampType.label, color = colors.inkMuted)
        Text(text = amount, style = RevampType.amount, color = amountColor, maxLines = 1)
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
        val state = ReportScreenState(
            baseCurrency = "INR",
            balance = -6405.66,
            income = 2000.0,
            expenses = 8405.66,
            upcomingIncome = 4800.23,
            upcomingExpenses = 0.0,
            overdueIncome = 2335.12,
            overdueExpenses = 0.0,
            history =
            persistentListOf(),
            upcomingTransactions = persistentListOf(),
            overdueTransactions = persistentListOf(),

            upcomingExpanded = true,
            overdueExpanded = true,
            filter = ReportFilter.emptyFilter("BGN"),
            loading = false,
            accounts = persistentListOf(
                acc1,
                acc2,
                Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                Account("Revolut", color = IvyDark.toArgb()),
            ),
            categories = persistentListOf(
                cat1,
                Category(
                    name = NotBlankTrimmedString.unsafe("Pet"),
                    color = ColorInt(Red3Light.toArgb()),
                    icon = IconAsset.unsafe("pet"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
                Category(
                    name = NotBlankTrimmedString.unsafe("Home"),
                    color = ColorInt(Green.toArgb()),
                    icon = null,
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
            ),
        )

        UI(state = state)
    }
}

@Preview
@Composable
private fun Preview_NO_FILTER(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())
        val cat1 = Category(
            name = NotBlankTrimmedString.unsafe("Science"),
            color = ColorInt(Purple1Dark.toArgb()),
            icon = IconAsset.unsafe("atom"),
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
        val state = ReportScreenState(
            baseCurrency = "INR",
            balance = 0.0,
            income = 0.0,
            expenses = 0.0,
            upcomingIncome = 0.0,
            upcomingExpenses = 0.0,
            overdueIncome = 0.0,
            overdueExpenses = 0.0,

            history = persistentListOf(),
            upcomingTransactions = persistentListOf(),
            overdueTransactions = persistentListOf(),

            upcomingExpanded = true,
            overdueExpanded = true,

            filter = null,
            loading = false,

            accounts = persistentListOf(
                acc1,
                acc2,
                Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                Account("Revolut", color = IvyDark.toArgb()),
            ),
            categories = persistentListOf(
                cat1,
                Category(
                    name = NotBlankTrimmedString.unsafe("Pet"),
                    color = ColorInt(Red3Light.toArgb()),
                    icon = IconAsset.unsafe("pet"),
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
                Category(
                    name = NotBlankTrimmedString.unsafe("Home"),
                    color = ColorInt(Green.toArgb()),
                    icon = null,
                    id = CategoryId(UUID.randomUUID()),
                    orderNum = 0.0,
                ),
            ),
        )

        UI(state = state)
    }
}

/** For screenshot testing */
@Composable
fun ReportUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    Preview(theme)
}

/** For screenshot testing */
@Composable
fun ReportNoFilterUiTest(isDark: Boolean) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    Preview_NO_FILTER(theme)
}