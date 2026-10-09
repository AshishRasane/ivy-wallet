package com.ivy.accounts

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.revamp.AmountFormat
import com.ivy.design.revamp.RevampCircleButton
import com.ivy.design.revamp.RevampTopBar
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountData
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.util.UUID

private val ScreenPadding = 20.dp
private const val NetWorthMutedAlpha = 0.72f

@Composable
fun BoxWithConstraintsScope.AccountsTab() {
    val viewModel: AccountsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: AccountsState,
    onEvent: (AccountsEvent) -> Unit = {}
) {
    val nav = navigation()
    val colors = revampColors()
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    val openAccount = { data: AccountData ->
        nav.navigateTo(TransactionsScreen(accountId = data.account.id.value, categoryId = null))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            RevampTopBar(title = "Accounts", onBack = { nav.back() }) {
                RevampCircleButton(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Reorder accounts",
                    onClick = { onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = true)) },
                )
            }
        }
        if (!state.hideTotalBalance) {
            item {
                NetWorthCard(
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp),
                    netWorth = AmountFormat.format(
                        amount = state.totalBalanceWithoutExcluded.toDoubleOrNull() ?: 0.0,
                        currency = state.baseCurrency,
                    ),
                    accounts = state.accountsData.size,
                    notCounted = state.accountsData.count { !it.account.includeInBalance },
                )
            }
        }
        items(state.accountsData, key = { it.account.id.value }) { data ->
            AccountCard(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                data = data,
                baseCurrency = state.baseCurrency,
                showMonth = !state.compactAccountsModeEnabled,
                isInvestment = data.account.id.value in state.investmentAccountIds,
                onClick = { openAccount(data) },
            )
        }
        item {
            AddAccountButton(
                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                onClick = {
                    accountModalData = AccountModalData(
                        account = null,
                        balance = 0.0,
                        baseCurrency = state.baseCurrency
                    )
                },
            )
        }
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = { onEvent(AccountsEvent.CreateAccount(it)) },
        onEditAccount = { _, _ -> },
        dismiss = { accountModalData = null }
    )

    ReorderModalSingleType(
        visible = state.reorderVisible,
        initialItems = state.accountsData,
        dismiss = {
            onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = false))
        },
        onReordered = {
            onEvent(AccountsEvent.OnReorder(reorderedList = it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.account.name.value,
            style = UI.typo.b1.style(
                color = item.account.color.value.toComposeColor(),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/** Inverted card (dark on light theme, light on dark theme), as in the mockup. */
@Composable
private fun NetWorthCard(
    netWorth: String,
    accounts: Int,
    notCounted: Int,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    val content = colors.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.ink)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "Net worth", style = RevampType.label, color = content.copy(alpha = NetWorthMutedAlpha))
        Text(text = netWorth, style = RevampType.display, color = content)
        Text(
            text = buildString {
                append(if (accounts == 1) "1 account" else "$accounts accounts")
                if (notCounted > 0) append(" · $notCounted not counted")
            },
            style = RevampType.label,
            color = content.copy(alpha = NetWorthMutedAlpha),
        )
    }
}

@Composable
private fun AccountCard(
    data: AccountData,
    baseCurrency: String,
    showMonth: Boolean,
    isInvestment: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    val account = data.account
    val currency = account.asset.code
    val accountColor = account.color.value.toComposeColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accountColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = account.name.value.take(1).uppercase(),
                style = RevampType.title,
                color = findContrastTextColor(accountColor),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = account.name.value,
                style = RevampType.bodyStrong,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    currency,
                    "Investment".takeIf { isInvestment },
                    "Not in net worth".takeIf { !account.includeInBalance },
                ).joinToString(" · "),
                style = RevampType.label,
                color = colors.inkMuted,
            )
            if (showMonth) {
                MonthLine(income = data.monthlyIncome, expenses = data.monthlyExpenses, currency = currency)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = AmountFormat.format(data.balance, currency),
                style = RevampType.bodyStrong,
                color = if (data.balance < 0) colors.expense else colors.ink,
            )
            val inBase = data.balanceBaseCurrency
            if (currency != baseCurrency && inBase != null) {
                Text(
                    text = AmountFormat.format(inBase, baseCurrency),
                    style = RevampType.label,
                    color = colors.inkMuted,
                )
            }
        }
    }
}

@Composable
private fun MonthLine(income: Double, expenses: Double, currency: String) {
    val colors = revampColors()
    Text(
        text = buildAnnotatedString {
            append("This month ")
            withStyle(SpanStyle(color = colors.income)) {
                append(AmountFormat.format(income, currency, signed = true, showDecimals = false))
            }
            append(" · ")
            withStyle(SpanStyle(color = colors.expense)) {
                append(AmountFormat.format(-expenses, currency, showDecimals = false))
            }
        },
        style = RevampType.caption,
        color = colors.inkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AddAccountButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = revampColors()
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .border(2.dp, colors.primary, shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onPrimaryTint)
        Spacer(Modifier.width(8.dp))
        Text(text = "Add account", style = RevampType.bodyStrong, color = colors.onPrimaryTint)
    }
}

@Suppress("MagicNumber")
private fun previewState(compact: Boolean): AccountsState {
    fun account(name: String, color: Color, currency: String = "INR", included: Boolean = true) = Account(
        id = AccountId(UUID.nameUUIDFromBytes(name.toByteArray())),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(color.toArgb()),
        asset = AssetCode.unsafe(currency),
        icon = null,
        includeInBalance = included,
        orderNum = 0.0,
    )
    return AccountsState(
        baseCurrency = "INR",
        accountsData = persistentListOf(
            AccountData(account("HDFC Savings", Color(0xFF1F4FA8)), 98420.15, null, 7599.0, 85000.0),
            AccountData(account("ICICI Credit Card", Color(0xFFA3410B)), -12859.75, null, 1231.0, 1100.0),
            AccountData(account("Cash", Color(0xFF0B7A55)), 39000.0, null, 0.0, 0.0),
            AccountData(
                account = account("Travel USD", Color(0xFF6B4DFF), "USD", included = false),
                balance = 120.0,
                balanceBaseCurrency = 10020.0,
                monthlyExpenses = 0.0,
                monthlyIncome = 0.0,
            ),
        ),
        totalBalanceWithExcluded = "134580.40",
        totalBalanceWithExcludedText = "INR 1,34,580.40",
        totalBalanceWithoutExcluded = "124560.40",
        totalBalanceWithoutExcludedText = "INR 1,24,560.40",
        reorderVisible = false,
        compactAccountsModeEnabled = compact,
        hideTotalBalance = false,
        investmentAccountIds = persistentSetOf(),
    )
}

@Preview
@Composable
private fun PreviewAccountsTabCompactModeDisabled(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        UI(state = previewState(compact = false))
    }
}

@Preview
@Composable
private fun PreviewAccountsTabCompactModeEnabled(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        UI(state = previewState(compact = true))
    }
}

/** For screen shot testing **/
@Composable
fun AccountsTabNonCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeDisabled(theme)
}

/** For screen shot testing **/
@Composable
fun AccountsTabCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeEnabled(theme)
}
