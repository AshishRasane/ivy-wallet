package com.ivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.revamp.AmountFormat
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.rootScreen
import com.ivy.legacy.ui.component.transaction.dueSections
import com.ivy.navigation.AccountsScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.transactions.revamp.TransactionDayGroup
import com.ivy.ui.R
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.wallet.ui.theme.modal.DeleteModal
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/** Space at the end of the list so the bottom bar never covers the last item. */
private val BottomBarClearance = 112.dp
private val ScreenPadding = 20.dp
private const val HiddenAmount = "••••••"

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.HomeTab() {
    val viewModel: HomeViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    HomeUi(uiState, viewModel::onEvent)
}

@Suppress("LongMethod")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.HomeUi(
    uiState: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    var choosePeriodModal: ChoosePeriodModalData? by remember { mutableStateOf(null) }
    var skipAllModalVisible by remember { mutableStateOf(false) }
    val currency = uiState.baseData.baseCurrency

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = BottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HomeTopBar(
                month = uiState.period.toDisplayShort(
                    startDateOfMonth = ivyContext.startDayOfMonth,
                    timeConverter = LocalTimeConverter.current,
                    timeProvider = LocalTimeProvider.current,
                    timeFormatter = LocalTimeFormatter.current,
                ),
                onPreviousMonth = { onEvent(HomeEvent.SelectPreviousMonth) },
                onNextMonth = { onEvent(HomeEvent.SelectNextMonth) },
                onMonthClick = { choosePeriodModal = ChoosePeriodModalData(period = uiState.period) },
                onSearch = { nav.navigateTo(SearchScreen) },
            )
        }
        item {
            BalanceBlock(
                balance = if (uiState.hideBalance) null else AmountFormat.format(uiState.balance.toDouble(), currency),
                accountsCount = uiState.baseData.accounts.size,
                onBalanceClick = {
                    onEvent(if (uiState.hideBalance) HomeEvent.HiddenBalanceClick else HomeEvent.BalanceClick)
                },
                onAccountsClick = { nav.navigateTo(AccountsScreen) },
            )
        }
        item {
            IncomeExpenseTiles(
                income = if (uiState.hideIncome) {
                    null
                } else {
                    AmountFormat.format(uiState.stats.income.toDouble(), currency)
                },
                expenses = AmountFormat.format(uiState.stats.expense.toDouble(), currency),
                onHiddenIncomeClick = { onEvent(HomeEvent.HiddenIncomeClick) },
            )
        }
        if (uiState.pendingSmsCount > 0) {
            item {
                SmsReviewBanner(
                    count = uiState.pendingSmsCount,
                    onClick = { onEvent(HomeEvent.ReviewSmsTransactions) },
                )
            }
        }

        dueSections(
            baseData = uiState.baseData,
            upcoming = uiState.upcoming,
            overdue = uiState.overdue,
            shouldShowAccountSpecificColorInTransactions = uiState.shouldShowAccountSpecificColorInTransactions,
            onPayOrGet = { onEvent(HomeEvent.PayOrGetPlanned(it)) },
            setUpcomingExpanded = { onEvent(HomeEvent.SetUpcomingExpanded(it)) },
            setOverdueExpanded = { onEvent(HomeEvent.SetOverdueExpanded(it)) },
            onSkipTransaction = { onEvent(HomeEvent.SkipPlanned(it)) },
            onSkipAllTransactions = { skipAllModalVisible = true },
        )

        item {
            RecentHeader(onSeeAll = { ivyContext.selectMainTab(MainTab.TRANSACTIONS) })
        }
        if (uiState.recent.isEmpty()) {
            item { EmptyRecent() }
        }
        items(uiState.recent, key = { "recent-${it.label}" }) { group ->
            TransactionDayGroup(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                group = group,
                onTransactionClick = { onEvent(HomeEvent.OpenTransaction(it)) },
            )
        }

        uiState.customerJourneyCards.firstOrNull()?.let { card ->
            item {
                // only available inside the app (not in previews), so it's read only when a tip shows
                val rootScreen = rootScreen()
                TipRow(
                    card = card,
                    onAction = { card.onAction(nav, ivyContext, rootScreen) },
                    onDismiss = { onEvent(HomeEvent.DismissCustomerJourneyCard(card)) },
                )
            }
        }
    }

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = { choosePeriodModal = null },
        onPeriodSelected = { onEvent(HomeEvent.SetPeriod(it)) },
    )

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = { skipAllModalVisible = false },
    ) {
        onEvent(HomeEvent.SkipAllPlanned(uiState.overdue.trns))
        skipAllModalVisible = false
    }
}

@Composable
private fun HomeTopBar(
    month: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onSearch: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier.padding(start = ScreenPadding - 8.dp, end = ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.ink)
        }
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Choose period", onClick = onMonthClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = month, style = RevampType.body, color = colors.ink)
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = colors.ink)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Search transactions", onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search transactions", tint = colors.ink)
        }
    }
}

@Composable
private fun BalanceBlock(
    /** Null when the balance is hidden. */
    balance: String?,
    accountsCount: Int,
    onBalanceClick: () -> Unit,
    onAccountsClick: () -> Unit,
) {
    val colors = revampColors()
    Column(
        modifier = Modifier.padding(horizontal = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "Total balance", style = RevampType.label, color = colors.inkMuted)
        Text(
            modifier = Modifier.clickable(
                role = Role.Button,
                onClickLabel = if (balance == null) "Show balance" else "Balance details",
                onClick = onBalanceClick,
            ),
            text = buildAnnotatedString {
                if (balance == null) {
                    append(HiddenAmount)
                } else {
                    // smaller, muted decimals: ₹1,24,560.40
                    val dot = balance.lastIndexOf('.')
                    append(if (dot >= 0) balance.substring(0, dot) else balance)
                    if (dot >= 0) {
                        withStyle(SpanStyle(fontSize = 24.sp, color = colors.inkMuted)) {
                            append(balance.substring(dot))
                        }
                    }
                }
            },
            style = RevampType.display,
            color = colors.ink,
        )
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clickable(role = Role.Button, onClick = onAccountsClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (accountsCount == 1) "Across 1 account" else "Across $accountsCount accounts",
                style = RevampType.body,
                color = colors.onPrimaryTint,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onPrimaryTint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun IncomeExpenseTiles(
    /** Null when income is hidden. */
    income: String?,
    expenses: String,
    onHiddenIncomeClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier.padding(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AmountTile(
            modifier = Modifier
                .weight(1f)
                .then(if (income == null) Modifier.clickable(onClick = onHiddenIncomeClick) else Modifier),
            icon = Icons.Filled.SouthWest,
            label = "Income",
            amount = income ?: HiddenAmount,
            accent = colors.income,
            accentTint = colors.incomeTint,
        )
        AmountTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.ArrowOutward,
            label = "Expenses",
            amount = expenses,
            accent = colors.expense,
            accentTint = colors.expenseTint,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun AmountTile(
    icon: ImageVector,
    label: String,
    amount: String,
    accent: Color,
    accentTint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = RevampType.label, color = accent)
        }
        Text(text = amount, style = RevampType.amount, color = colors.ink, maxLines = 1)
    }
}

@Composable
private fun SmsReviewBanner(
    count: Int,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.primaryTint)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MarkChatUnread,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (count == 1) "1 bank SMS ready to add" else "$count bank SMS ready to add",
                style = RevampType.bodyStrong,
                color = colors.onPrimaryTint,
            )
            Text(text = "Tap to add or ignore", style = RevampType.label, color = colors.onPrimaryTint)
        }
        Text(text = "Review", style = RevampType.bodyStrong, color = colors.onPrimaryTint)
    }
}

@Composable
private fun RecentHeader(onSeeAll: () -> Unit) {
    val colors = revampColors()
    Row(
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding - 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(modifier = Modifier.weight(1f), text = "Recent", style = RevampType.title, color = colors.ink)
        Box(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onSeeAll)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "See all", style = RevampType.bodyStrong, color = colors.onPrimaryTint)
        }
    }
}

@Composable
private fun EmptyRecent() {
    val colors = revampColors()
    Text(
        modifier = Modifier.padding(horizontal = ScreenPadding),
        text = "No transactions this month yet. Tap + to add one.",
        style = RevampType.body,
        color = colors.inkMuted,
    )
}

/** A compact, dismissible tip that replaces the old full-width journey cards. */
@Composable
private fun TipRow(
    card: CustomerJourneyCardModel,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .padding(horizontal = ScreenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onAction)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Lightbulb,
            contentDescription = null,
            tint = colors.onPrimaryTint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
        ) {
            Text(text = card.title, style = RevampType.bodyStrong, color = colors.ink)
            if (card.cta != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = card.cta, style = RevampType.label, color = colors.onPrimaryTint)
            }
        }
        if (card.hasDismiss) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss tip", tint = colors.inkMuted)
            }
        } else {
            Spacer(Modifier.width(14.dp))
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.PreviewHomeTab(isDark: Boolean = false) {
    IvyPreview(isDark) {
        HomeUi(
            uiState = HomeState(
                theme = Theme.AUTO,
                name = "",
                baseData = AppBaseData(
                    baseCurrency = "INR",
                    accounts = persistentListOf(),
                    categories = persistentListOf()
                ),
                balance = BigDecimal("124560.40"),
                buffer = BufferInfo(
                    amount = BigDecimal.ZERO,
                    bufferDiff = BigDecimal.ZERO,
                ),
                customerJourneyCards = persistentListOf(),
                history = persistentListOf(),
                stats = IncomeExpensePair(income = BigDecimal("85000"), expense = BigDecimal("32418.50")),
                upcoming = LegacyDueSection(
                    trns = persistentListOf(),
                    stats = IncomeExpensePair.zero(),
                    expanded = false,
                ),
                overdue = LegacyDueSection(
                    trns = persistentListOf(),
                    stats = IncomeExpensePair.zero(),
                    expanded = false,
                ),
                period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                hideBalance = false,
                hideIncome = false,
                expanded = false,
                shouldShowAccountSpecificColorInTransactions = false,
                pendingSmsCount = 2,
                recent = persistentListOf(),
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        PreviewHomeTab(isDark)
    }
}
