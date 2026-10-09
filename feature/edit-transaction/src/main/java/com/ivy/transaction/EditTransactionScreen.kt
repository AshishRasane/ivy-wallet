package com.ivy.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.l0_system.Orange
import com.ivy.design.revamp.PlusJakartaSans
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.design.utils.hideKeyboard
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.EditTransactionDisplayLoan
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.tags.ShowTagModal
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.transaction.revamp.AmountInput
import com.ivy.transaction.revamp.PickerOption
import com.ivy.transaction.revamp.PickerSheet
import com.ivy.ui.R
import com.ivy.wallet.domain.data.CustomExchangeRateState
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.components.CustomExchangeRateCard
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.ProgressModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModal
import com.ivy.wallet.ui.theme.modal.edit.CategoryModalData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

private val ScreenPadding = 20.dp
private const val MaxTitleSuggestions = 3

private enum class AccountPicker { NONE, FROM, TO }

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.EditTransactionScreen(screen: EditTransactionScreen) {
    val viewModel: EditTransactionViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    LaunchedEffect(Unit) {
        viewModel.start(screen)
    }

    UI(screen = screen, state = uiState, onEvent = viewModel::onEvent)
}

/**
 * One screen for adding and editing a transaction: amount with keypad, account, category,
 * date/time, title, note and tags are all visible at once.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BoxWithConstraintsScope.UI(
    screen: EditTransactionScreen,
    state: EditTransactionViewState,
    onEvent: (EditTransactionViewEvent) -> Unit,
) {
    val colors = revampColors()
    val nav = navigation()
    val view = LocalView.current
    val ivyContext = ivyWalletCtx()
    val timeConverter = LocalTimeConverter.current
    val isEdit = screen.initialTransactionId != null
    val type = state.transactionType
    val loan = state.displayLoanHelper
    val decimals = IvyCurrency.getDecimalPlaces(state.currency)

    // The typed amount. In edit mode it's sent on Save, so the view model doesn't save on every key.
    var amountText by remember(screen.initialTransactionId) { mutableStateOf(AmountInput.fromAmount(state.amount)) }
    LaunchedEffect(state.amount) {
        if (AmountInput.toAmount(amountText) != state.amount) {
            amountText = AmountInput.fromAmount(state.amount)
        }
    }
    val typedAmount = AmountInput.toAmount(amountText)

    var accountPicker by remember { mutableStateOf(AccountPicker.NONE) }
    var categoryPickerVisible by remember { mutableStateOf(false) }
    var categoryModalData: CategoryModalData? by remember { mutableStateOf(null) }
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var deleteModalVisible by remember { mutableStateOf(false) }
    var tagsModalVisible by remember { mutableStateOf(false) }
    var calculatorVisible by remember { mutableStateOf(false) }
    var exchangeRateModalVisible by remember { mutableStateOf(false) }
    var loanAccountChange: Account? by remember { mutableStateOf(null) }
    val calculatorModalId = remember(screen.initialTransactionId) { UUID.randomUUID() }
    val exchangeRateModalId = remember(screen.initialTransactionId, state.customExchangeRateState.exchangeRate) {
        UUID.randomUUID()
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    // a new, undated expense/income can still become a planned payment
    val canBecomePlanned = !isEdit && state.dueDate == null &&
        type != TransactionType.TRANSFER && state.dateTime == null

    // Read at click time: the Save button may keep an older click handler when nothing on it changed.
    val currentState by rememberUpdatedState(state)

    fun changeAccount(account: Account) {
        val latest = currentState
        if (latest.displayLoanHelper.isLoan && latest.account?.currency != account.currency) {
            loanAccountChange = account
        } else {
            onEvent(EditTransactionViewEvent.OnAccountChanged(account))
        }
    }

    fun save() {
        view.hideKeyboard()
        val latest = currentState
        val amountNow = AmountInput.toAmount(amountText)
        if (amountNow != latest.amount) onEvent(EditTransactionViewEvent.OnAmountChanged(amountNow))
        when {
            isEdit && latest.dueDate != null && !latest.hasChanges -> {
                onEvent(EditTransactionViewEvent.OnPayPlannedPayment)
            }
            isEdit && latest.dueDate != null -> {
                onEvent(EditTransactionViewEvent.Save(closeScreen = false))
                onEvent(EditTransactionViewEvent.SetHasChanges(false))
            }
            else -> onEvent(EditTransactionViewEvent.Save(closeScreen = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        TopBar(
            type = type,
            // a loan record's type can't be changed
            canChangeType = !loan.isLoanRecord,
            isEdit = isEdit,
            onClose = {
                view.hideKeyboard()
                nav.back()
            },
            onTypeChange = { onEvent(EditTransactionViewEvent.OnSetTransactionType(it)) },
            onDuplicate = { onEvent(EditTransactionViewEvent.Duplicate) },
            onDelete = { deleteModalVisible = true },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (screen.smsTransactionId != null) {
                SmsSourceBanner()
            }

            AmountBlock(
                label = when (type) {
                    TransactionType.INCOME -> "You received"
                    TransactionType.EXPENSE -> "You paid"
                    TransactionType.TRANSFER -> "You moved"
                },
                amount = AmountInput.display(amountText, indianGrouping = state.currency == "INR"),
                currency = state.currency,
                color = when (type) {
                    TransactionType.INCOME -> colors.income
                    TransactionType.EXPENSE -> colors.expense
                    TransactionType.TRANSFER -> colors.ink
                },
                onCalculator = { calculatorVisible = true },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldTile(
                    label = when (type) {
                        TransactionType.INCOME -> "Received in"
                        TransactionType.EXPENSE -> "Paid from"
                        TransactionType.TRANSFER -> "From"
                    },
                    value = state.account?.name ?: "Choose account",
                    dotColor = state.account?.color?.let(::Color),
                    onClick = { accountPicker = AccountPicker.FROM },
                )
                if (type == TransactionType.TRANSFER) {
                    FieldTile(
                        label = "To",
                        value = state.toAccount?.name ?: "Choose account",
                        dotColor = state.toAccount?.color?.let(::Color),
                        onClick = { accountPicker = AccountPicker.TO },
                    )
                } else {
                    FieldTile(
                        label = "Category",
                        value = state.category?.name?.value ?: "Choose category",
                        dotColor = state.category?.color?.value?.let(::Color),
                        onClick = { categoryPickerVisible = true },
                    )
                }
            }

            val dateTime = state.dateTime?.let { with(timeConverter) { it.toLocalDateTime() } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldTile(
                    label = "Date",
                    value = dateTime?.let { dayLabel(it) } ?: "Today",
                    dotColor = null,
                    onClick = { onEvent(EditTransactionViewEvent.OnChangeDate) },
                )
                FieldTile(
                    label = "Time",
                    value = dateTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "Now",
                    dotColor = null,
                    onClick = { onEvent(EditTransactionViewEvent.OnChangeTime) },
                )
            }

            TitleField(
                initialTitle = state.initialTitle,
                suggestions = state.titleSuggestions.take(MaxTitleSuggestions).toImmutableList(),
                onTitleChange = { onEvent(EditTransactionViewEvent.OnTitleChanged(it)) },
            )

            NoteField(
                initialNote = state.description,
                onNoteChange = { onEvent(EditTransactionViewEvent.OnDescriptionChanged(it)) },
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    icon = { Icon(Icons.Filled.Sell, contentDescription = null, tint = colors.onPrimaryTint) },
                    text = if (state.transactionAssociatedTags.isEmpty()) {
                        "Add tags"
                    } else {
                        "Tags · ${state.transactionAssociatedTags.size}"
                    },
                    onClick = { tagsModalVisible = true },
                )
                if (canBecomePlanned) {
                    Chip(
                        icon = {
                            Icon(Icons.Filled.EventRepeat, contentDescription = null, tint = colors.onPrimaryTint)
                        },
                        text = "Make it a planned payment",
                        onClick = {
                            nav.back()
                            nav.navigateTo(
                                EditPlannedScreen(
                                    plannedPaymentRuleId = null,
                                    type = type,
                                    amount = typedAmount,
                                    accountId = state.account?.id,
                                    categoryId = state.category?.id?.value,
                                    title = state.initialTitle,
                                    description = state.description,
                                )
                            )
                        },
                    )
                }
            }

            if (state.dueDate != null) {
                val dueDate = state.dueDate
                WideFieldTile(
                    label = "Due date",
                    value = with(timeConverter) { dueDate.toLocalDateTime() }.let { dayLabel(it) },
                    dotColor = null,
                    onClick = {
                        ivyContext.datePicker(initialDate = with(timeConverter) { dueDate.toLocalDate() }) {
                            onEvent(EditTransactionViewEvent.OnDueDateChanged(it.atTime(12, 0)))
                        }
                    },
                )
            }

            loan.loanCaption?.let { caption ->
                Text(text = caption, style = RevampType.label, color = colors.inkMuted)
            }

            if (type == TransactionType.TRANSFER && state.customExchangeRateState.showCard) {
                CustomExchangeRateCard(
                    fromCurrencyCode = state.currency,
                    toCurrencyCode = state.customExchangeRateState.toCurrencyCode ?: state.currency,
                    exchangeRate = state.customExchangeRateState.exchangeRate,
                    onRefresh = { onEvent(EditTransactionViewEvent.UpdateExchangeRate(null)) },
                ) {
                    exchangeRateModalVisible = true
                }
            }
        }

        if (!imeVisible) {
            Keypad(
                standardLayout = state.standardKeypad,
                onKey = { key ->
                    amountText = AmountInput.press(amountText, key, decimals)
                    if (!isEdit) onEvent(EditTransactionViewEvent.OnAmountChanged(AmountInput.toAmount(amountText)))
                },
            )
        }

        SaveButton(
            text = saveLabel(type = type, isEdit = isEdit, dueDate = state.dueDate, hasChanges = state.hasChanges),
            enabled = typedAmount > 0.0 &&
                state.account != null &&
                (type != TransactionType.TRANSFER || state.toAccount != null),
            onClick = ::save,
        )
    }

    if (accountPicker != AccountPicker.NONE) {
        val picking = accountPicker
        PickerSheet(
            title = if (picking == AccountPicker.TO) "To account" else "Account",
            options = state.accounts.map {
                PickerOption(key = it.id.toString(), label = it.name, detail = it.currency, color = it.color)
            }.toImmutableList(),
            selectedKey = (if (picking == AccountPicker.TO) state.toAccount else state.account)?.id?.toString(),
            newLabel = "New account",
            onSelect = { key ->
                state.accounts.firstOrNull { it.id.toString() == key }?.let {
                    if (picking == AccountPicker.TO) {
                        onEvent(EditTransactionViewEvent.OnToAccountChanged(it))
                    } else {
                        changeAccount(it)
                    }
                }
                accountPicker = AccountPicker.NONE
            },
            onNew = {
                accountPicker = AccountPicker.NONE
                accountModalData = AccountModalData(account = null, baseCurrency = state.currency, balance = 0.0)
            },
            onDismiss = { accountPicker = AccountPicker.NONE },
        )
    }

    if (categoryPickerVisible) {
        PickerSheet(
            title = "Category",
            options = state.categories.map {
                PickerOption(key = it.id.value.toString(), label = it.name.value, detail = null, color = it.color.value)
            }.toImmutableList(),
            selectedKey = state.category?.id?.value?.toString(),
            newLabel = "New category",
            noneLabel = "No category",
            onSelect = { key ->
                onEvent(
                    EditTransactionViewEvent.OnCategoryChanged(
                        state.categories.firstOrNull { it.id.value.toString() == key }
                    )
                )
                categoryPickerVisible = false
            },
            onNew = {
                categoryPickerVisible = false
                categoryModalData = CategoryModalData(category = null)
            },
            onDismiss = { categoryPickerVisible = false },
        )
    }

    CategoryModal(
        modal = categoryModalData,
        onCreateCategory = { onEvent(EditTransactionViewEvent.CreateCategory(it)) },
        onEditCategory = { onEvent(EditTransactionViewEvent.EditCategory(it)) },
        dismiss = { categoryModalData = null },
    )

    AccountModal(
        modal = accountModalData,
        onCreateAccount = { onEvent(EditTransactionViewEvent.CreateAccount(it)) },
        onEditAccount = { _, _ -> },
        dismiss = { accountModalData = null },
    )

    DeleteModal(
        visible = deleteModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = stringResource(R.string.transaction_confirm_deletion_description),
        dismiss = { deleteModalVisible = false },
    ) {
        onEvent(EditTransactionViewEvent.Delete)
    }

    DeleteModal(
        visible = loanAccountChange != null,
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_change_description),
        buttonText = stringResource(R.string.confirm),
        iconStart = R.drawable.ic_agreed,
        dismiss = { loanAccountChange = null },
    ) {
        loanAccountChange?.let { onEvent(EditTransactionViewEvent.OnAccountChanged(it)) }
        loanAccountChange = null
    }

    ProgressModal(
        title = stringResource(R.string.confirm_account_change),
        description = stringResource(R.string.confirm_account_loan_change),
        visible = state.backgroundProcessingStarted,
    )

    AmountModal(
        id = calculatorModalId,
        visible = calculatorVisible,
        currency = state.currency,
        initialAmount = typedAmount,
        dismiss = { calculatorVisible = false },
        decimalCountMax = decimals,
        onAmountChanged = {
            amountText = AmountInput.fromAmount(it)
            if (!isEdit) onEvent(EditTransactionViewEvent.OnAmountChanged(it))
        },
    )

    AmountModal(
        id = exchangeRateModalId,
        visible = exchangeRateModalVisible,
        currency = "",
        initialAmount = state.customExchangeRateState.exchangeRate,
        dismiss = { exchangeRateModalVisible = false },
        decimalCountMax = IvyCurrency.getDecimalPlaces(
            state.customExchangeRateState.toCurrencyCode ?: state.currency
        ),
        onAmountChanged = { onEvent(EditTransactionViewEvent.UpdateExchangeRate(it)) },
    )

    ShowTagModal(
        visible = tagsModalVisible,
        onDismiss = {
            tagsModalVisible = false
            // reset the search so the full list shows next time
            onEvent(EditTransactionViewEvent.TagEvent.OnTagSearch(""))
        },
        allTagList = state.tags,
        selectedTagList = state.transactionAssociatedTags,
        onTagAdd = { onEvent(EditTransactionViewEvent.TagEvent.SaveTag(name = it)) },
        onTagEdit = { oldTag, newTag -> onEvent(EditTransactionViewEvent.TagEvent.OnTagEdit(oldTag, newTag)) },
        onTagDelete = { onEvent(EditTransactionViewEvent.TagEvent.OnTagDelete(it)) },
        onTagSelected = { onEvent(EditTransactionViewEvent.TagEvent.OnTagSelect(it)) },
        onTagDeSelected = { onEvent(EditTransactionViewEvent.TagEvent.OnTagDeSelect(it)) },
        onTagSearch = { onEvent(EditTransactionViewEvent.TagEvent.OnTagSearch(it)) },
    )
}

private fun saveLabel(type: TransactionType, isEdit: Boolean, dueDate: Instant?, hasChanges: Boolean): String = when {
    isEdit && dueDate != null && !hasChanges -> if (type == TransactionType.EXPENSE) "Pay now" else "Mark as received"
    isEdit -> "Save changes"
    type == TransactionType.INCOME -> "Save income"
    type == TransactionType.TRANSFER -> "Save transfer"
    else -> "Save expense"
}

@Composable
private fun dayLabel(dateTime: LocalDateTime): String {
    val today = LocalTimeProvider.current.localDateNow()
    return when (dateTime.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> dateTime.format(
            DateTimeFormatter.ofPattern(if (dateTime.year == today.year) "EEE, d MMM" else "d MMM yyyy")
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun TopBar(
    type: TransactionType,
    canChangeType: Boolean,
    isEdit: Boolean,
    onClose: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = revampColors()
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = colors.ink)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(colors.border)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                TransactionType.EXPENSE to "Expense",
                TransactionType.INCOME to "Income",
                TransactionType.TRANSFER to "Transfer",
            ).forEach { (option, label) ->
                val selected = option == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.surface else Color.Transparent)
                        .clickable(enabled = canChangeType && !selected, role = Role.RadioButton) {
                            onTypeChange(option)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = RevampType.body,
                        color = if (selected) colors.ink else colors.inkMuted,
                    )
                }
            }
        }
        if (isEdit) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = colors.ink)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = colors.expense) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun SmsSourceBanner() {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.primaryTint)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MarkChatRead,
            contentDescription = null,
            tint = colors.onPrimaryTint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = "Filled from your bank SMS", style = RevampType.label, color = colors.onPrimaryTint)
    }
}

@Composable
private fun AmountBlock(
    label: String,
    amount: String,
    currency: String,
    color: Color,
    onCalculator: () -> Unit,
) {
    val colors = revampColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, style = RevampType.label, color = colors.inkMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.semantics { contentDescription = "$label $amount $currency" },
                text = if (currency == "INR") "₹$amount" else "$amount $currency",
                style = RevampType.display.copy(fontSize = 40.sp),
                color = color,
                maxLines = 1,
            )
            IconButton(onClick = onCalculator) {
                Icon(Icons.Filled.Calculate, contentDescription = "Calculator", tint = colors.inkMuted)
            }
        }
    }
}

@Composable
private fun RowScope.FieldTile(
    label: String,
    value: String,
    dotColor: Color?,
    onClick: () -> Unit,
) {
    FieldTileContent(
        modifier = Modifier.weight(1f),
        label = label,
        value = value,
        dotColor = dotColor,
        onClick = onClick,
    )
}

@Composable
private fun WideFieldTile(
    label: String,
    value: String,
    dotColor: Color?,
    onClick: () -> Unit,
) {
    FieldTileContent(
        modifier = Modifier.fillMaxWidth(),
        label = label,
        value = value,
        dotColor = dotColor,
        onClick = onClick,
    )
}

@Composable
private fun FieldTileContent(
    label: String,
    value: String,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = label, style = RevampType.caption, color = colors.inkMuted)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(text = value, style = RevampType.bodyStrong, color = colors.ink, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleField(
    initialTitle: String?,
    suggestions: ImmutableList<String>,
    onTitleChange: (String?) -> Unit,
) {
    val colors = revampColors()
    var text by remember(initialTitle == null) { mutableStateOf(initialTitle.orEmpty()) }
    LaunchedEffect(initialTitle) {
        if (initialTitle != null && initialTitle != text) text = initialTitle
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InputBox(label = "Title") {
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = {
                    text = it
                    onTitleChange(it.ifBlank { null })
                },
                singleLine = true,
                textStyle = inputStyle(colors.ink),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { field ->
                    if (text.isEmpty()) Text("e.g. Swiggy", style = inputStyle(colors.inkMuted))
                    field()
                },
            )
        }
        val visible = suggestions.filter { !it.equals(text, ignoreCase = true) }
        if (visible.isNotEmpty() && text.isNotBlank()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { suggestion ->
                    Chip(icon = null, text = suggestion) {
                        text = suggestion
                        onTitleChange(suggestion)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteField(
    initialNote: String?,
    onNoteChange: (String?) -> Unit,
) {
    val colors = revampColors()
    var text by remember(initialNote == null) { mutableStateOf(initialNote.orEmpty()) }
    LaunchedEffect(initialNote) {
        if (initialNote != null && initialNote != text) text = initialNote
    }
    InputBox(label = "Note") {
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = {
                text = it
                onNoteChange(it.ifBlank { null })
            },
            textStyle = inputStyle(colors.ink).copy(fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { field ->
                if (text.isEmpty()) {
                    Text("Optional", style = inputStyle(colors.inkMuted).copy(fontWeight = FontWeight.Medium))
                }
                field()
            },
        )
    }
}

private fun inputStyle(color: Color): TextStyle =
    TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)

@Composable
private fun InputBox(label: String, content: @Composable () -> Unit) {
    val colors = revampColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = label, style = RevampType.caption, color = colors.inkMuted)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun Chip(
    icon: (@Composable () -> Unit)?,
    text: String,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(colors.primaryTint)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = RevampType.label.copy(fontWeight = FontWeight.Bold), color = colors.onPrimaryTint)
    }
}

@Composable
private fun Keypad(
    standardLayout: Boolean,
    onKey: (String) -> Unit,
) {
    val colors = revampColors()
    val digitRows = if (standardLayout) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    } else {
        listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"))
    }
    Column(
        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (digitRows + listOf(listOf(AmountInput.DOT, "0", AmountInput.DELETE))).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .clickable(role = Role.Button) { onKey(key) }
                            .semantics {
                                contentDescription = when (key) {
                                    AmountInput.DELETE -> "Delete digit"
                                    AmountInput.DOT -> "Decimal point"
                                    else -> key
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == AmountInput.DELETE) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = colors.ink)
                        } else {
                            Text(text = key, style = RevampType.title.copy(fontSize = 22.sp), color = colors.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .padding(horizontal = ScreenPadding, vertical = 10.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.primary else colors.border)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = if (enabled) colors.onPrimary else colors.inkMuted)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = RevampType.bodyStrong, color = if (enabled) colors.onPrimary else colors.inkMuted)
    }
}

/** For Preview purpose **/
private val testDateTime = LocalDateTime.of(2023, 4, 27, 0, 35).toInstant(ZoneOffset.UTC)

@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.Preview(isDark: Boolean = false) {
    IvyPreview(isDark) {
        UI(
            screen = EditTransactionScreen(null, TransactionType.EXPENSE),
            state = EditTransactionViewState(
                transactionType = TransactionType.EXPENSE,
                initialTitle = "Swiggy",
                titleSuggestions = persistentSetOf(),
                currency = "INR",
                description = null,
                dateTime = testDateTime,
                dueDate = null,
                accounts = persistentListOf(),
                categories = persistentListOf(),
                account = Account(name = "HDFC Savings", color = Orange.toArgb()),
                toAccount = null,
                category = null,
                amount = 2499.0,
                hasChanges = false,
                displayLoanHelper = EditTransactionDisplayLoan(),
                backgroundProcessingStarted = false,
                customExchangeRateState = CustomExchangeRateState(),
                tags = persistentListOf(),
                transactionAssociatedTags = persistentListOf(),
                standardKeypad = false,
            ),
            onEvent = {},
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditTransactionScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        Preview(isDark)
    }
}
