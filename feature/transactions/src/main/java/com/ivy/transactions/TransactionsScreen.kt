package com.ivy.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.ivy.design.revamp.AmountFormat
import com.ivy.design.revamp.RevampCircleButton
import com.ivy.design.revamp.RevampTopBar
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.legacy.ui.component.transaction.dueSections
import com.ivy.transactions.revamp.TransactionDayGroup
import com.ivy.transactions.revamp.AmountTone
import com.ivy.transactions.revamp.TransactionDayGroupUi
import com.ivy.transactions.revamp.TransactionRowUi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.stringRes
import com.ivy.base.model.TransactionType
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.utils.thenIf
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.setStatusBarDarkTextCompat
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.wallet.ui.theme.modal.DeleteConfirmationModal
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.roundToLong

@Composable
fun BoxWithConstraintsScope.TransactionsScreen(screen: TransactionsScreen) {
    val viewModel: TransactionsViewModel = screenScopedViewModel()

    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    val uiState = viewModel.uiState()

    val view = LocalView.current
    LaunchedEffect(Unit) {
        viewModel.start(screen)

        nav.onBackPressed[screen] = {
            setStatusBarDarkTextCompat(
                view = view,
                darkText = ivyContext.theme == Theme.LIGHT
            )
            false
        }
    }

    UI(
        screen = screen,
        period = uiState.period,
        baseCurrency = uiState.baseCurrency,
        currency = uiState.currency,

        categories = uiState.categories,
        accounts = uiState.accounts,

        account = uiState.account,
        category = uiState.category,

        balance = uiState.balance,
        balanceBaseCurrency = uiState.balanceBaseCurrency,
        income = uiState.income,
        expenses = uiState.expenses,

        initWithTransactions = uiState.initWithTransactions,
        treatTransfersAsIncomeExpense = uiState.treatTransfersAsIncomeExpense,

        historyGroups = uiState.historyGroups,
        shouldShowAccountSpecificColorInTransactions = uiState.showAccountColorsInTransactions,

        upcoming = uiState.upcoming,
        upcomingExpanded = uiState.upcomingExpanded,
        setUpcomingExpanded = {
            viewModel.onEvent(TransactionsEvent.SetUpcomingExpanded(it))
        },
        upcomingIncome = uiState.upcomingIncome,
        upcomingExpenses = uiState.upcomingExpenses,

        overdue = uiState.overdue,
        overdueExpanded = uiState.overdueExpanded,
        setOverdueExpanded = {
            viewModel.onEvent(TransactionsEvent.SetOverdueExpanded(it))
        },
        overdueIncome = uiState.overdueIncome,
        overdueExpenses = uiState.overdueExpenses,

        onSetPeriod = {
            viewModel.onEvent(
                TransactionsEvent.SetPeriod(
                    screen = screen,
                    period = it
                )
            )
        },
        onNextMonth = {
            viewModel.onEvent(TransactionsEvent.NextMonth(screen))
        },
        onPreviousMonth = {
            viewModel.onEvent(TransactionsEvent.PreviousMonth(screen))
        },
        onDelete = {
            viewModel.onEvent(TransactionsEvent.Delete(screen))
        },
        onEditCategory = {
            viewModel.onEvent(TransactionsEvent.EditCategory(it))
        },
        onEditAccount = { acc, newBalance ->
            viewModel.onEvent(TransactionsEvent.EditAccount(screen, acc, newBalance))
        },
        onPayOrGet = { transaction ->
            viewModel.onEvent(TransactionsEvent.PayOrGet(screen, transaction))
        },
        onSkipTransaction = { transaction ->
            viewModel.onEvent(TransactionsEvent.SkipTransaction(screen, transaction))
        },
        onSkipAllTransactions = { transactions ->
            viewModel.onEvent(TransactionsEvent.SkipTransactions(screen, transactions))
        },
        updateAccountNameConfirmation = {
            viewModel.onEvent(TransactionsEvent.UpdateAccountDeletionState(it))
        },
        enableDeletionButton = uiState.enableDeletionButton,
        skipAllModalVisible = uiState.skipAllModalVisible,
        onSkipAllModalVisible = {
            viewModel.onEvent(TransactionsEvent.SetSkipAllModalVisible(it))
        },
        deleteModal1Visible = uiState.deleteModal1Visible,
        onDeleteModal1Visible = {
            viewModel.onEvent(TransactionsEvent.OnDeleteModal1Visible(it))
        },
        onChoosePeriodModal = {
            viewModel.onEvent(TransactionsEvent.OnChoosePeriodModalData(it))
        },
        choosePeriodModal = uiState.choosePeriodModal,
        lastBillPaymentAccountId = uiState.lastBillPaymentAccountId,
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun BoxWithConstraintsScope.UI(
    screen: TransactionsScreen,
    period: TimePeriod,
    baseCurrency: String,
    currency: String,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,

    account: Account?,
    category: Category?,

    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,

    categories: ImmutableList<Category>,
    accounts: ImmutableList<Account>,

    balance: Double,
    balanceBaseCurrency: Double?,
    income: Double,
    expenses: Double,
    choosePeriodModal: ChoosePeriodModalData?,

    historyGroups: ImmutableList<TransactionDayGroupUi>,
    shouldShowAccountSpecificColorInTransactions: Boolean,
    lastBillPaymentAccountId: AccountId?,

    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetPeriod: (TimePeriod) -> Unit,
    onEditAccount: (Account, Double) -> Unit,
    onEditCategory: (Category) -> Unit,
    onDelete: () -> Unit,
    deleteModal1Visible: Boolean,
    onDeleteModal1Visible: (Boolean) -> Unit,

    initWithTransactions: Boolean = false,
    treatTransfersAsIncomeExpense: Boolean = false,
    upcomingExpanded: Boolean = true,
    setUpcomingExpanded: (Boolean) -> Unit = {},
    upcomingIncome: Double = 0.0,
    upcomingExpenses: Double = 0.0,
    upcoming: ImmutableList<Transaction> = persistentListOf(),

    overdueExpanded: Boolean = true,
    setOverdueExpanded: (Boolean) -> Unit = {},
    overdueIncome: Double = 0.0,
    overdueExpenses: Double = 0.0,
    overdue: ImmutableList<Transaction> = persistentListOf(),

    onPayOrGet: (Transaction) -> Unit = {},
    onSkipTransaction: (Transaction) -> Unit = {},
    onSkipAllTransactions: (List<Transaction>) -> Unit = {},
    onChoosePeriodModal: (ChoosePeriodModalData?) -> Unit,
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    val colors = revampColors()
    val itemColor = (account?.color ?: category?.color?.value)?.toComposeColor() ?: Gray

    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    val editAccount = { adjustBalance: Boolean ->
        if (account != null) {
            accountModalData = AccountModalData(
                account = account,
                baseCurrency = currency,
                balance = balance,
                adjustBalanceMode = adjustBalance,
                autoFocusKeyboard = false
            )
        }
    }
    val openPieChart = { type: TransactionType ->
        if (account != null) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = type,
                    accountList = persistentListOf(account.id),
                    filterExcluded = false,
                    treatTransfersAsIncomeExpense = treatTransfersAsIncomeExpense
                )
            )
        }
    }
    // opened from a transfer in the pie chart: editing or deleting the account/category doesn't apply
    val canEdit = (account != null || category != null) &&
        screen.transactions.none { it.type == TransactionType.TRANSFER }

    val timeProvider = LocalTimeProvider.current
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current
    val swipeListenerState = rememberSwipeListenerState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .thenIf(!initWithTransactions) {
                horizontalSwipeListener(
                    sensitivity = 150,
                    state = swipeListenerState,
                    onSwipeLeft = onNextMonth,
                    onSwipeRight = onPreviousMonth,
                )
            }
            .testTag("item_stats_lazy_column"),
        state = rememberScrollPositionListState(key = "item_stats_lazy_column"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RevampTopBar(
                title = account?.name ?: category?.name?.value ?: Constants.CATEGORY_UNSPECIFIED_NAME,
                onBack = { nav.back() },
            ) {
                if (canEdit) {
                    RevampCircleButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        onClick = {
                            if (account != null) {
                                editAccount(false)
                            } else {
                                categoryModalData = CategoryModalData(category = category, autoFocusKeyboard = false)
                            }
                        },
                    )
                    RevampCircleButton(
                        icon = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        onClick = { onDeleteModal1Visible(true) },
                    )
                }
            }
        }
        item {
            SummaryCard(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                itemColor = itemColor,
                isAccount = account != null,
                excluded = account?.includeInBalance == false,
                headline = if (account != null) {
                    AmountFormat.format(balance, currency)
                } else {
                    AmountFormat.format(income - expenses, currency, signed = true)
                },
                inBaseCurrency = balanceBaseCurrency
                    ?.takeIf { account != null && currency != baseCurrency }
                    ?.let { AmountFormat.format(it, baseCurrency) },
                income = AmountFormat.format(income, currency),
                expenses = AmountFormat.format(expenses, currency),
                onBalanceClick = { editAccount(true) },
                // a card (or any account) in debt: pay it off from another account in one step
                payBill = AmountFormat.format(-balance, currency).takeIf { account != null && balance < 0 },
                onPayBill = {
                    nav.navigateTo(
                        EditTransactionScreen(
                            initialTransactionId = null,
                            type = TransactionType.TRANSFER,
                            accountId = lastBillPaymentAccountId?.value
                                ?: accounts.firstOrNull { it.id != account?.id && it.includeInBalance }?.id,
                            toAccountId = account?.id,
                            amount = (-balance * 100).roundToLong() / 100.0,
                            title = "Card bill",
                        )
                    )
                },
                onIncomeClick = { openPieChart(TransactionType.INCOME) },
                onExpensesClick = { openPieChart(TransactionType.EXPENSE) },
                onAdd = { type ->
                    nav.navigateTo(
                        EditTransactionScreen(
                            initialTransactionId = null,
                            type = type,
                            accountId = account?.id,
                            categoryId = category?.id?.value
                        )
                    )
                },
            )
        }
        if (!initWithTransactions) {
            item {
                PeriodSwitcher(
                    period = period.toDisplayShort(
                        startDateOfMonth = ivyContext.startDayOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider,
                        timeFormatter = timeFormatter,
                    ),
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onClick = { onChoosePeriodModal(ChoosePeriodModalData(period = period)) },
                )
            }
        }

        dueSections(
            baseData = AppBaseData(baseCurrency, accounts, categories),
            upcoming = LegacyDueSection(
                trns = upcoming,
                stats = IncomeExpensePair(
                    income = upcomingIncome.toBigDecimal(),
                    expense = upcomingExpenses.toBigDecimal()
                ),
                expanded = upcomingExpanded
            ),
            overdue = LegacyDueSection(
                trns = overdue,
                stats = IncomeExpensePair(
                    income = overdueIncome.toBigDecimal(),
                    expense = overdueExpenses.toBigDecimal()
                ),
                expanded = overdueExpanded
            ),
            shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
            onPayOrGet = onPayOrGet,
            setUpcomingExpanded = setUpcomingExpanded,
            setOverdueExpanded = setOverdueExpanded,
            onSkipTransaction = onSkipTransaction,
            onSkipAllTransactions = { onSkipAllModalVisible(true) },
        )

        if (historyGroups.isEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = 32.dp),
                    text = stringRes(R.string.no_transactions),
                    style = RevampType.body,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(historyGroups, key = { "day-${it.label}" }) { group ->
            TransactionDayGroup(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                group = group,
                onTransactionClick = { row ->
                    nav.navigateTo(EditTransactionScreen(initialTransactionId = row.id, type = row.type))
                },
            )
        }
    }

    DeleteModals(
        account = account,
        category = category,
        updateAccountNameConfirmation = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        onDelete = onDelete,
        skipAllModalVisible = skipAllModalVisible,
        onSkipAllModalVisible = {
            onSkipAllModalVisible(it)
        },
        onSkipAllTransactions = onSkipAllTransactions,
        deleteModal1Visible = deleteModal1Visible,
        setDeleteModal1Visible = onDeleteModal1Visible
    )

    CategoryModal(
        modal = categoryModalData,
        onCreateCategory = { },
        onEditCategory = onEditCategory,
        dismiss = {
            categoryModalData = null
        }
    )

    AccountModal(
        modal = accountModalData,
        onCreateAccount = { },
        onEditAccount = onEditAccount,
        dismiss = {
            accountModalData = null
        }
    )

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            onChoosePeriodModal(null)
        }
    ) {
        onSetPeriod(it)
    }
}

@Composable
private fun BoxWithConstraintsScope.DeleteModals(
    deleteModal1Visible: Boolean,
    setDeleteModal1Visible: (Boolean) -> Unit,
    account: Account?,
    category: Category?,
    updateAccountNameConfirmation: (String) -> Unit,
    enableDeletionButton: Boolean,
    onDelete: () -> Unit,
    skipAllModalVisible: Boolean,
    onSkipAllModalVisible: (Boolean) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    overdue: ImmutableList<Transaction> = persistentListOf(),
) {
    var deleteModal3Visible by remember { mutableStateOf(false) }

    DeleteModal(
        visible = deleteModal1Visible,
        title = stringResource(R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(R.string.account_confirm_deletion_description)
        } else {
            stringResource(R.string.category_confirm_deletion_description)
        },
        dismiss = {
            setDeleteModal1Visible(false)
        }
    ) {
        deleteModal3Visible = true
    }

    DeleteConfirmationModal(
        visible = deleteModal3Visible,
        title = stringResource(id = R.string.confirm_deletion),
        description = if (account != null) {
            stringResource(
                id = R.string.account_confirm_deletion_type_account_name,
                account.name
            )
        } else {
            stringResource(R.string.please_type_category_name, category?.name?.value ?: "")
        },
        hint = if (account != null) stringResource(id = R.string.account_name) else "Category name",
        onAccountNameChange = updateAccountNameConfirmation,
        enableDeletionButton = enableDeletionButton,
        dismiss = {
            updateAccountNameConfirmation("")
            deleteModal3Visible = false
            setDeleteModal1Visible(false)
        }
    ) {
        onDelete()
        updateAccountNameConfirmation("")
        setDeleteModal1Visible(false)
    }

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            onSkipAllModalVisible(false)
        }
    ) {
        onSkipAllTransactions(overdue)
        onSkipAllModalVisible(false)
    }
}

private val ScreenPadding = 20.dp

/** Balance (account) or net amount (category), this period's income/expenses and add buttons. */
@Suppress("LongParameterList")
@Composable
private fun SummaryCard(
    itemColor: Color,
    isAccount: Boolean,
    excluded: Boolean,
    headline: String,
    inBaseCurrency: String?,
    income: String,
    expenses: String,
    onBalanceClick: () -> Unit,
    /** "₹6,046.00" to pay off, or null when the account isn't in debt */
    payBill: String?,
    onPayBill: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(itemColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    !isAccount -> "Net this period"
                    excluded -> "Balance · Not in net worth"
                    else -> "Balance"
                },
                style = RevampType.label,
                color = colors.inkMuted,
            )
        }
        Column(
            modifier = Modifier
                .thenIf(isAccount) {
                    clickable(role = Role.Button, onClickLabel = "Adjust balance", onClick = onBalanceClick)
                }
        ) {
            Text(text = headline, style = RevampType.display, color = colors.ink)
            if (inBaseCurrency != null) {
                Text(text = inBaseCurrency, style = RevampType.label, color = colors.inkMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowTile(
                modifier = Modifier.weight(1f),
                label = "Income",
                amount = income,
                amountColor = colors.income,
                tint = colors.incomeTint,
                onClick = onIncomeClick.takeIf { isAccount },
            )
            FlowTile(
                modifier = Modifier.weight(1f),
                label = "Expenses",
                amount = expenses,
                amountColor = colors.expense,
                tint = colors.expenseTint,
                onClick = onExpensesClick.takeIf { isAccount },
            )
        }
        if (payBill != null) {
            PayBillButton(amount = payBill, onClick = onPayBill)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AddButton(modifier = Modifier.weight(1f), label = "Add expense") { onAdd(TransactionType.EXPENSE) }
            AddButton(modifier = Modifier.weight(1f), label = "Add income") { onAdd(TransactionType.INCOME) }
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
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint)
            .thenIf(onClick != null) { clickable(role = Role.Button, onClick = { onClick?.invoke() }) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = RevampType.label, color = colors.inkMuted)
        Text(text = amount, style = RevampType.amount, color = amountColor, maxLines = 1)
    }
}

@Composable
private fun AddButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = revampColors()
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(colors.primaryTint)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onPrimaryTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = RevampType.body, color = colors.onPrimaryTint)
    }
}

@Composable
private fun PayBillButton(amount: String, onClick: () -> Unit) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = "Pay bill · $amount", style = RevampType.bodyStrong, color = colors.onPrimary)
    }
}

@Composable
private fun PeriodSwitcher(
    period: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding - 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous period", tint = colors.ink)
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
            Text(text = period, style = RevampType.body, color = colors.ink)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next period", tint = colors.ink)
        }
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_empty() {
    IvyPreview {
        UI(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1
            ), // preview
            baseCurrency = "BGN",
            currency = "BGN",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = null,
            income = 8000.0,
            expenses = 6000.0,

            historyGroups = persistentListOf(),
            category = null,
            account = Account("DSK", color = GreenDark.toArgb(), icon = "pet"),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _ -> },
            onEditCategory = {},
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false,
            lastBillPaymentAccountId = null,
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_crypto() {
    IvyPreview {
        UI(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1
            ), // preview
            baseCurrency = "BGN",
            currency = "ADA",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = 2879.28,
            income = 8000.0,
            expenses = 6000.0,

            historyGroups = persistentListOf(),
            category = null,
            account = Account(
                name = "DSK",
                color = GreenDark.toArgb(),
                icon = "pet",
                includeInBalance = false
            ),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _ -> },
            onEditCategory = {},
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false,
            lastBillPaymentAccountId = null,
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_empty_upcoming() {
    IvyPreview {
        UI(
            period = TimePeriod(month = Month.monthsList().first(), year = 2023),
            baseCurrency = "INR",
            currency = "INR",

            categories = persistentListOf(),
            accounts = persistentListOf(),

            balance = 1314.578,
            balanceBaseCurrency = null,
            income = 8000.0,
            expenses = 6000.0,

            historyGroups = persistentListOf(
                TransactionDayGroupUi(
                    label = "Today",
                    total = "−₹2,885.00",
                    totalTone = AmountTone.EXPENSE,
                    rows = persistentListOf(
                        TransactionRowUi(
                            id = UUID(1L, 3L),
                            type = TransactionType.EXPENSE,
                            title = "Swiggy",
                            subtitle = "Food & Drinks · HDFC Savings",
                            initial = "S",
                            amount = "−₹2,499.00",
                            tone = AmountTone.EXPENSE,
                            avatarColor = 0xFFA3410B.toInt(),
                        ),
                        TransactionRowUi(
                            id = UUID(1L, 4L),
                            type = TransactionType.EXPENSE,
                            title = "Uber",
                            subtitle = "Transport · HDFC Savings",
                            initial = "U",
                            amount = "−₹386.00",
                            tone = AmountTone.EXPENSE,
                            avatarColor = 0xFF1F4FA8.toInt(),
                        ),
                    ),
                ),
            ),
            category = null,
            account = Account("HDFC Savings", color = GreenDark.toArgb(), icon = "pet"),
            onSetPeriod = { },
            onPreviousMonth = {},
            onNextMonth = {},
            onDelete = {},
            onEditAccount = { _, _ -> },
            onEditCategory = {},
            upcoming = persistentListOf(
                Transaction(
                    UUID(1L, 2L),
                    TransactionType.EXPENSE,
                    BigDecimal.valueOf(10L)
                )
            ),
            updateAccountNameConfirmation = {},
            enableDeletionButton = true,
            deleteModal1Visible = false,
            onDeleteModal1Visible = {},
            skipAllModalVisible = false,
            onSkipAllModalVisible = {},
            onChoosePeriodModal = {},
            choosePeriodModal = null,
            screen = TransactionsScreen(),
            shouldShowAccountSpecificColorInTransactions = false,
            lastBillPaymentAccountId = null,
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        Preview_empty_upcoming()
    }
}