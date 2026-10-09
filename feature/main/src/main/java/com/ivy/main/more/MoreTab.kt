package com.ivy.main.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.ivy.design.revamp.RevampRow
import com.ivy.design.revamp.RevampSection
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.navigation.AccountsScreen
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.SmsReviewScreen
import com.ivy.navigation.screenScopedViewModel
import com.ivy.wallet.ui.theme.modal.BufferModal

private val BottomBarClearance = 112.dp

private data class MoreRow(
    val icon: ImageVector,
    val label: String,
    val value: String?,
    val onClick: () -> Unit,
)

@Composable
fun BoxWithConstraintsScope.MoreTab(
    viewModel: MoreViewModel = screenScopedViewModel(),
) {
    val state = viewModel.uiState()
    MoreTabUi(state = state, onEvent = viewModel::onEvent)

    BufferModal(
        modal = state.savingsGoalModal,
        dismiss = { viewModel.onEvent(MoreEvent.DismissSavingsGoal) },
        onBufferChanged = { viewModel.onEvent(MoreEvent.SetSavingsGoal(it)) },
    )
}

@Composable
private fun MoreTabUi(
    state: MoreState,
    onEvent: (MoreEvent) -> Unit,
) {
    val colors = revampColors()
    val open = { screen: com.ivy.navigation.Screen -> onEvent(MoreEvent.Open(screen)) }
    val sections = listOf(
        "Money" to persistentListOf(
            MoreRow(Icons.Filled.AccountBalanceWallet, "Accounts", value = null) { open(AccountsScreen) },
            MoreRow(Icons.Filled.Category, "Categories", value = null) { open(CategoriesScreen) },
            MoreRow(Icons.Filled.PieChart, "Budgets", value = null) { open(BudgetScreen) },
            MoreRow(Icons.Filled.Handshake, "Loans", value = null) { open(LoansScreen) },
            MoreRow(Icons.Filled.EventRepeat, "Planned payments", value = null) { open(PlannedPaymentsScreen) },
            MoreRow(Icons.Filled.Flag, "Savings goal", state.savingsGoal) { onEvent(MoreEvent.EditSavingsGoal) },
        ),
        "Your data" to persistentListOf(
            MoreRow(Icons.Filled.Backup, "Backup & export", "Auto: ${state.autoBackup}") { open(SettingsScreen) },
            MoreRow(Icons.Filled.FileDownload, "Import data", value = null) {
                open(ImportScreen(launchedFromOnboarding = false))
            },
        ),
        "App" to listOfNotNull(
            MoreRow(Icons.Filled.Sms, "SMS detection", state.smsDetection) { open(FeaturesScreen) },
            MoreRow(Icons.Filled.MarkChatUnread, "Review bank SMS", state.pendingSms.toString()) {
                open(SmsReviewScreen)
            }.takeIf { state.pendingSms > 0 },
            MoreRow(Icons.Filled.DarkMode, "Theme", state.theme) { onEvent(MoreEvent.SwitchTheme) },
            MoreRow(Icons.Filled.Settings, "Settings", value = null) { open(SettingsScreen) },
        ).toImmutableList(),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = BottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(text = "More", style = RevampType.headline, color = colors.ink)
        }
        sections.forEach { (title, rows) ->
            item(key = title) { MoreSection(title = title, rows = rows) }
        }
    }
}

@Composable
private fun MoreSection(title: String, rows: ImmutableList<MoreRow>) {
    val colors = revampColors()
    RevampSection(title = title) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = colors.divider)
            RevampRow(icon = row.icon, label = row.label, value = row.value, onClick = row.onClick)
        }
    }
}
