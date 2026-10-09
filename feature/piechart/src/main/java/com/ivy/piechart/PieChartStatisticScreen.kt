package com.ivy.piechart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.revamp.AmountFormat
import com.ivy.design.revamp.RevampColors
import com.ivy.design.revamp.RevampTopBar
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import kotlin.math.roundToInt

private val ScreenPadding = 20.dp
private const val TintAlpha = 0.18f
private const val FullCircle = 360f
private const val StartAngle = -90f
private const val SliceGap = 2f
private const val MinBar = 0.02f
private const val Percent = 100
private const val MinSweep = 0.5f

/** A category's share of the month, ready to draw. */
@Immutable
private data class Slice(
    val name: String,
    val amount: String,
    val percent: String,
    /** 0..1 of the total, for the donut */
    val share: Float,
    /** 0..1 of the largest category, for the bar */
    val bar: Float,
    val color: Color,
    val target: SliceTarget,
)

/** What tapping a row opens. */
@Immutable
private sealed interface SliceTarget {
    data class Category(val categoryAmount: CategoryAmount) : SliceTarget

    // AccountId is a typed value-class id; the detekt rule doesn't recognize it.
    @Suppress("DataClassTypedIDs")
    data class Account(val accountId: AccountId) : SliceTarget
}

@Composable
fun BoxWithConstraintsScope.PieChartStatisticScreen(
    screen: PieChartStatisticScreen
) {
    val viewModel: PieChartStatisticViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(PieChartStatisticEvent.OnStart(screen))
    }

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Suppress("LongMethod")
@Composable
private fun BoxWithConstraintsScope.UI(
    state: PieChartStatisticState,
    onEvent: (PieChartStatisticEvent) -> Unit = {}
) {
    val nav = navigation()
    val colors = revampColors()
    val isExpense = state.transactionType == TransactionType.EXPENSE
    val view = when {
        state.invested -> ReportView.Invested
        isExpense -> ReportView.Expenses
        else -> ReportView.Income
    }
    val slices = if (state.invested) investedSlices(state, colors) else slices(state, colors)
    val (accent, soft) = view.colors(colors)
    // opened with a fixed list of transactions (from an account's transfers): no period or type to change
    val fixedTransactions = state.showCloseButtonOnly

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            RevampTopBar(title = "Reports", onBack = { nav.back() })
        }
        if (!fixedTransactions) {
            item {
                PeriodSwitcher(
                    period = state.period,
                    onPrevious = { onEvent(PieChartStatisticEvent.OnSelectPreviousMonth) },
                    onNext = { onEvent(PieChartStatisticEvent.OnSelectNextMonth) },
                    onClick = { onEvent(PieChartStatisticEvent.OnShowMonthModal(state.period)) },
                )
            }
            item {
                TypeSwitch(selected = view, onSelect = { onEvent(it.toEvent()) })
            }
        }
        item {
            DonutCard(
                slices = slices,
                label = view.totalLabel(state.totalAmount),
                total = AmountFormat.format(abs(state.totalAmount), state.baseCurrency),
                totalColor = if (state.invested) colors.onPrimaryTint else accent,
                count = countLabel(state.transactionCount, state.invested, isExpense),
                comparison = state.comparison,
            )
        }
        if (slices.isNotEmpty()) {
            item {
                CategoryList(
                    title = if (state.invested) "By account" else "By category",
                    slices = slices,
                    onClick = { target -> nav.navigateTo(target.screen(state.accountIdFilterList)) },
                )
            }
        } else if (state.invested) {
            item { NoInvestments() }
        }
        if (state.trend.isNotEmpty()) {
            item {
                TrendCard(
                    bars = state.trend,
                    average = state.trendAverage,
                    accent = accent,
                    soft = soft,
                )
            }
        }
        if (!fixedTransactions && state.accountIdFilterList.isEmpty()) {
            item {
                CustomReportRow(onClick = { nav.navigateTo(ReportScreen) })
            }
        }
    }

    ChoosePeriodModal(
        modal = state.choosePeriodModal,
        dismiss = {
            onEvent(PieChartStatisticEvent.OnShowMonthModal(null))
        }
    ) {
        onEvent(PieChartStatisticEvent.OnSetPeriod(it))
    }
}

private fun slices(state: PieChartStatisticState, colors: RevampColors): ImmutableList<Slice> {
    val visible = state.categoryAmounts.filter { it.amount > 0.0 }.sortedByDescending { it.amount }
    val total = visible.sumOf { it.amount }.takeIf { it > 0.0 } ?: return persistentListOf()
    val largest = visible.first().amount
    return visible.map { item ->
        val share = item.amount / total
        Slice(
            name = item.category?.name?.value ?: "Unspecified",
            amount = AmountFormat.format(item.amount, state.baseCurrency),
            percent = "${(share * Percent).roundToInt().coerceAtLeast(1)}%",
            share = share.toFloat(),
            bar = (item.amount / largest).toFloat().coerceAtLeast(MinBar),
            color = item.category?.color?.value?.let { Color(it) } ?: colors.inkMuted,
            target = SliceTarget.Category(item),
        )
    }.toImmutableList()
}

/** Accounts with money put in, largest first; the donut shows only those, withdrawals are listed after. */
private fun investedSlices(state: PieChartStatisticState, colors: RevampColors): ImmutableList<Slice> {
    val inTotal = state.investedAccounts.filter { it.amount > 0 }.sumOf { it.amount }
    val largest = state.investedAccounts.maxOfOrNull { abs(it.amount) }?.takeIf { it > 0 } ?: return persistentListOf()
    return state.investedAccounts.filter { it.amount != 0.0 }.map { account ->
        val share = if (account.amount > 0 && inTotal > 0) account.amount / inTotal else 0.0
        Slice(
            name = account.name,
            amount = AmountFormat.format(account.amount, state.baseCurrency),
            percent = if (share > 0) "${(share * Percent).roundToInt().coerceAtLeast(1)}%" else "Withdrawn",
            share = share.toFloat(),
            bar = (abs(account.amount) / largest).toFloat().coerceAtLeast(MinBar),
            color = if (account.amount > 0) Color(account.color) else colors.inkMuted,
            target = SliceTarget.Account(account.accountId),
        )
    }.toImmutableList()
}

private fun countLabel(count: Int, invested: Boolean, isExpense: Boolean): String {
    val noun = if (invested) "transfer" else "transaction"
    return when {
        count == 0 && invested -> "No transfers"
        count == 0 -> if (isExpense) "No expenses" else "No income"
        count == 1 -> "1 $noun"
        else -> "$count ${noun}s"
    }
}

private enum class ReportView(val label: String) {
    Expenses("Expenses"),
    Income("Income"),
    Invested("Invested");

    fun toEvent(): PieChartStatisticEvent = when (this) {
        Expenses -> PieChartStatisticEvent.OnTypeChanged(TransactionType.EXPENSE)
        Income -> PieChartStatisticEvent.OnTypeChanged(TransactionType.INCOME)
        Invested -> PieChartStatisticEvent.OnInvestedSelected
    }

    fun totalLabel(total: Double): String = when (this) {
        Expenses -> "Spent"
        Income -> "Earned"
        Invested -> if (total < 0) "Withdrawn" else "Invested"
    }

    /** Strong and soft color: chart bars, totals. */
    fun colors(colors: RevampColors): Pair<Color, Color> = when (this) {
        Expenses -> colors.expense to colors.expenseTint
        Income -> colors.income to colors.incomeTint
        Invested -> colors.primary to colors.primaryTint
    }
}

private fun SliceTarget.screen(accountIdFilterList: List<UUID>): TransactionsScreen = when (this) {
    is SliceTarget.Account -> TransactionsScreen(accountId = accountId.value)
    is SliceTarget.Category -> TransactionsScreen(
        categoryId = categoryAmount.category?.id?.value,
        unspecifiedCategory = categoryAmount.isCategoryUnspecified,
        accountIdFilterList = accountIdFilterList,
        transactions = categoryAmount.associatedTransactions
    )
}

@Composable
private fun NoInvestments() {
    val colors = revampColors()
    Text(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(16.dp),
        text = "No money moved into investment accounts this month. To count an account (e.g. Zerodha, " +
            "Kuvera) as an investment, open it from Accounts and turn on \"Count as investment\".",
        style = RevampType.label,
        color = colors.inkMuted,
    )
}

@Composable
private fun PeriodSwitcher(
    period: TimePeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    val ivyContext = ivyWalletCtx()
    val label = period.toDisplayShort(
        startDateOfMonth = ivyContext.startDayOfMonth,
        timeConverter = LocalTimeConverter.current,
        timeProvider = LocalTimeProvider.current,
        timeFormatter = LocalTimeFormatter.current,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding - 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.ink)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Choose period", onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = RevampType.body, color = colors.ink)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = colors.ink)
        }
    }
}

@Composable
private fun TypeSwitch(selected: ReportView, onSelect: (ReportView) -> Unit) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReportView.entries.forEach { view ->
            val isSelected = view == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) colors.ink else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(view) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = view.label,
                    style = RevampType.bodyStrong,
                    color = if (isSelected) colors.surface else colors.ink,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun DonutCard(
    slices: ImmutableList<Slice>,
    label: String,
    total: String,
    totalColor: Color,
    count: String,
    comparison: String?,
) {
    val colors = revampColors()
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Donut(slices = slices, empty = colors.divider)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = RevampType.label, color = colors.inkMuted)
                Text(
                    modifier = Modifier.testTag("piechart_total_amount"),
                    text = total,
                    style = RevampType.title,
                    color = totalColor,
                )
                Text(text = count, style = RevampType.caption, color = colors.inkMuted)
            }
        }
        if (comparison != null) {
            Text(text = comparison, style = RevampType.label, color = colors.inkMuted)
        }
    }
}

@Composable
private fun Donut(slices: ImmutableList<Slice>, empty: Color) {
    val description = slices.joinToString { "${it.name} ${it.percent}" }
    Canvas(
        modifier = Modifier
            .size(200.dp)
            .semantics { contentDescription = description }
    ) {
        val stroke = 22.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(empty, 0f, FullCircle, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        val gap = if (slices.size > 1) SliceGap else 0f
        var start = StartAngle
        slices.forEach { slice ->
            val sweep = slice.share * FullCircle
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = (sweep - gap).coerceAtLeast(MinSweep),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke),
            )
            start += sweep
        }
    }
}

@Composable
private fun CategoryList(title: String, slices: ImmutableList<Slice>, onClick: (SliceTarget) -> Unit) {
    val colors = revampColors()
    Column(
        modifier = Modifier.padding(horizontal = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = title,
            style = RevampType.caption,
            color = colors.inkMuted,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
        ) {
            slices.forEachIndexed { index, slice ->
                if (index > 0) HorizontalDivider(color = colors.divider)
                CategoryRow(slice = slice, onClick = { onClick(slice.target) })
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: Slice, onClick: () -> Unit) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(slice.color.copy(alpha = TintAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(slice.color)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = slice.name,
                    style = RevampType.bodyStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = slice.percent, style = RevampType.label, color = colors.inkMuted)
                Spacer(Modifier.width(8.dp))
                Text(text = slice.amount, style = RevampType.bodyStrong, color = colors.ink)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.divider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(slice.bar)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
            }
        }
    }
}

@Composable
private fun TrendCard(bars: ImmutableList<TrendBar>, average: String?, accent: Color, soft: Color) {
    val colors = revampColors()
    Column(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Last ${bars.size} months",
                style = RevampType.bodyStrong,
                color = colors.ink,
            )
            if (average != null) {
                Text(text = average, style = RevampType.label, color = colors.inkMuted)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = "${bar.month} ${bar.amount}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                ) {
                    Text(
                        text = bar.amount,
                        style = RevampType.caption,
                        color = if (bar.current) colors.ink else colors.inkMuted,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((BarMaxHeight * bar.fraction).coerceAtLeast(BarMinHeight).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (bar.current) accent else soft)
                    )
                    Text(
                        text = bar.month,
                        style = RevampType.caption,
                        color = if (bar.current) colors.ink else colors.inkMuted,
                    )
                }
            }
        }
    }
}

private const val BarMaxHeight = 80f
private const val BarMinHeight = 4f

@Composable
private fun CustomReportRow(onClick: () -> Unit) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primaryTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.FilterAlt, contentDescription = null, tint = colors.onPrimaryTint)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Custom report", style = RevampType.bodyStrong, color = colors.ink)
            Text(
                text = "Filter by account, category, tags or dates · export CSV",
                style = RevampType.label,
                color = colors.inkMuted,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.inkMuted)
    }
}

@Suppress("MagicNumber")
private fun previewState(type: TransactionType): PieChartStatisticState {
    fun category(name: String, color: Long) = Category(
        id = CategoryId(UUID.nameUUIDFromBytes(name.toByteArray())),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(Color(color).toArgb()),
        icon = null,
        orderNum = 0.0,
    )
    val expense = type == TransactionType.EXPENSE
    val amounts = if (expense) {
        persistentListOf(
            CategoryAmount(category("Food & Drinks", 0xFFA3410B), 7420.0),
            CategoryAmount(category("Shopping", 0xFF6B2BB0), 6240.0),
            CategoryAmount(category("Bills", 0xFF7A5A00), 5860.0),
            CategoryAmount(category("Transport", 0xFF1F4FA8), 3911.0),
            CategoryAmount(category("Groceries", 0xFF0B7A55), 3240.0),
            CategoryAmount(null, 1760.0, isCategoryUnspecified = true),
        )
    } else {
        persistentListOf(
            CategoryAmount(category("Salary", 0xFF0B7A55), 85000.0),
            CategoryAmount(category("Refunds", 0xFF1F4FA8), 1100.0),
        )
    }
    val months = ReportMath.lastMonths(java.time.YearMonth.of(2026, 10), 6)
    val totals = if (expense) {
        listOf(31200.0, 27450.0, 35800.0, 29900.0, 32300.0, 28431.0)
    } else {
        listOf(85000.0, 85600.0, 85000.0, 91200.0, 85300.0, 86100.0)
    }
    return PieChartStatisticState(
        transactionType = type,
        period = TimePeriod(month = Month(10, "October"), year = 2026),
        baseCurrency = "INR",
        totalAmount = amounts.sumOf { it.amount },
        categoryAmounts = amounts,
        selectedCategory = null,
        accountIdFilterList = persistentListOf(),
        showCloseButtonOnly = false,
        filterExcluded = false,
        transactions = persistentListOf(),
        choosePeriodModal = null,
        trend = ReportMath.trend(months, totals, "INR").toImmutableList(),
        trendAverage = "5-month avg ₹31.3k",
        comparison = ReportMath.comparison(totals[5], totals[4], months[4]),
        transactionCount = if (expense) 64 else 2,
        invested = false,
        investedAccounts = persistentListOf(),
    )
}

@Preview
@Composable
private fun Preview_Expense(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(state = previewState(TransactionType.EXPENSE))
    }
}

@Preview
@Composable
private fun Preview_Income(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(state = previewState(TransactionType.INCOME))
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Preview_Invested(theme: Theme = Theme.LIGHT) {
    fun account(name: String, color: Long, amount: Double) = InvestedAccount(
        accountId = AccountId(UUID.nameUUIDFromBytes(name.toByteArray())),
        name = name,
        color = Color(color).toArgb(),
        amount = amount,
    )
    val accounts = persistentListOf(
        account("Kuvera", 0xFF0B7A55, 25000.0),
        account("Groww", 0xFF1F4FA8, 6000.0),
        account("Zerodha", 0xFF6B2BB0, -4000.0),
    )
    val months = ReportMath.lastMonths(java.time.YearMonth.of(2026, 10), 6)
    val totals = listOf(20000.0, 26000.0, 15000.0, 31000.0, 24000.0, 27000.0)
    IvyWalletPreview(theme) {
        UI(
            state = previewState(TransactionType.EXPENSE).copy(
                invested = true,
                investedAccounts = accounts,
                totalAmount = accounts.sumOf { it.amount },
                categoryAmounts = persistentListOf(),
                transactionCount = 4,
                trend = ReportMath.trend(months, totals, "INR").toImmutableList(),
                trendAverage = "5-month avg ₹23.2k",
                comparison = ReportMath.comparison(totals[5], totals[4], months[4]),
            )
        )
    }
}

/** For screenshot testing */
@Composable
fun PieChartStatisticUiTest(isDark: Boolean, income: Boolean = false, invested: Boolean = false) {
    val theme = if (isDark) Theme.DARK else Theme.LIGHT
    when {
        invested -> Preview_Invested(theme)
        income -> Preview_Income(theme)
        else -> Preview_Expense(theme)
    }
}
