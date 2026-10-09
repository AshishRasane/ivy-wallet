package com.ivy.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Attribution
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.design.revamp.RevampRow
import com.ivy.design.revamp.RevampSection
import com.ivy.design.revamp.RevampTopBar
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.legacy.Constants
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.rootScreen
import com.ivy.navigation.AttributionsScreen
import com.ivy.navigation.ContributorsScreen
import com.ivy.navigation.ExchangeRatesScreen
import com.ivy.navigation.FeaturesScreen
import com.ivy.navigation.ImportScreen
import com.ivy.navigation.ReleasesScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.modal.ChooseStartDateOfMonthModal
import com.ivy.wallet.ui.theme.modal.CurrencyModal
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.NameModal
import com.ivy.wallet.ui.theme.modal.ProgressModal
import java.util.Locale

private val ScreenPadding = 20.dp

@Composable
fun BoxWithConstraintsScope.SettingsScreen() {
    val viewModel: SettingsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()
    val rootScreen = rootScreen()
    val backupFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        folderUri?.let { viewModel.onEvent(SettingsEvent.AutoBackupFolderSelected(it)) }
    }

    UI(
        state = uiState,
        version = "${rootScreen.buildVersionName} (${rootScreen.buildVersionCode})",
        onEvent = viewModel::onEvent,
        onBackupData = { viewModel.onEvent(SettingsEvent.BackupData(rootScreen)) },
        onExportToCSV = { viewModel.onEvent(SettingsEvent.ExportToCsv(rootScreen)) },
        onSetAutoBackup = { enabled ->
            if (enabled && !uiState.autoBackup.hasFolder) {
                // the folder must be chosen first; selecting it turns automatic backup on
                backupFolderPicker.launch(null)
            } else {
                viewModel.onEvent(SettingsEvent.SetAutoBackup(enabled))
            }
        },
        onPickBackupFolder = { backupFolderPicker.launch(null) },
        onOpenRepo = { rootScreen.openUrlInBrowser(url = Constants.URL_IVY_WALLET_REPO) },
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun BoxWithConstraintsScope.UI(
    state: SettingsState,
    version: String,
    onEvent: (SettingsEvent) -> Unit,
    onBackupData: () -> Unit,
    onExportToCSV: () -> Unit,
    onSetAutoBackup: (Boolean) -> Unit,
    onPickBackupFolder: () -> Unit,
    onOpenRepo: () -> Unit,
) {
    var currencyModalVisible by remember { mutableStateOf(false) }
    var nameModalVisible by remember { mutableStateOf(false) }
    var chooseStartDateOfMonthVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalVisible by remember { mutableStateOf(false) }
    var deleteAllDataModalFinalVisible by remember { mutableStateOf(false) }
    val nav = navigation()
    val colors = revampColors()
    val autoBackup = state.autoBackup

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            RevampTopBar(title = stringResource(R.string.settings), onBack = { nav.onBackPressed() })
        }

        item {
            SettingsSection("General") {
                RevampRow(
                    icon = Icons.Filled.Payments,
                    label = "Currency",
                    value = state.currencyCode,
                    onClick = { currencyModalVisible = true },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.Person,
                    label = "Your name",
                    value = state.name.ifBlank { "Not set" },
                    onClick = { nameModalVisible = true },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.DarkMode,
                    label = "Theme",
                    value = themeLabel(state.currentTheme),
                    onClick = { onEvent(SettingsEvent.SwitchTheme) },
                )
                if (state.languageOptionVisible) {
                    Divider()
                    RevampRow(
                        icon = Icons.Filled.Language,
                        label = stringResource(R.string.language),
                        value = Locale.getDefault().displayName,
                        onClick = { onEvent(SettingsEvent.SwitchLanguage) },
                    )
                }
                Divider()
                RevampRow(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Start of month",
                    value = state.startDateOfMonth,
                    onClick = { chooseStartDateOfMonthVisible = true },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.CurrencyExchange,
                    label = stringResource(R.string.exchange_rates),
                    onClick = { nav.navigateTo(ExchangeRatesScreen) },
                )
            }
        }

        item {
            SettingsSection("Your data") {
                SwitchRow(
                    icon = Icons.Filled.Backup,
                    label = "Automatic backup",
                    detail = "Daily, to a folder on this phone. Keeps the last 10.",
                    checked = autoBackup.enabled,
                    onCheckedChange = onSetAutoBackup,
                )
                if (autoBackup.enabled || autoBackup.hasFolder) {
                    Divider()
                    RevampRow(
                        icon = Icons.Filled.Folder,
                        label = "Backup folder",
                        detail = autoBackup.folder ?: "Not selected",
                        onClick = onPickBackupFolder,
                    )
                    Divider()
                    RevampRow(
                        icon = Icons.Filled.Sync,
                        label = if (autoBackup.inProgress) "Backing up…" else "Back up now",
                        detail = autoBackup.status,
                        onClick = { if (!autoBackup.inProgress) onEvent(SettingsEvent.BackupNow) },
                    )
                }
                Divider()
                RevampRow(
                    icon = Icons.Filled.Save,
                    label = "Back up to a file",
                    detail = "A .zip you can restore with Import data",
                    onClick = onBackupData,
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.FileDownload,
                    label = stringResource(R.string.import_data),
                    detail = "Restore a backup or import a CSV",
                    onClick = { nav.navigateTo(ImportScreen(launchedFromOnboarding = false)) },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.TableChart,
                    label = "Export to CSV",
                    detail = "For spreadsheets, not for restoring",
                    onClick = onExportToCSV,
                )
            }
        }

        item {
            SettingsSection("Privacy & display") {
                SwitchRow(
                    icon = Icons.Filled.Fingerprint,
                    label = stringResource(R.string.lock_app),
                    checked = state.lockApp,
                    onCheckedChange = { onEvent(SettingsEvent.SetLockApp(it)) },
                )
                Divider()
                SwitchRow(
                    icon = Icons.Filled.Notifications,
                    label = stringResource(R.string.show_notifications),
                    checked = state.showNotifications,
                    onCheckedChange = { onEvent(SettingsEvent.SetShowNotifications(it)) },
                )
                Divider()
                SwitchRow(
                    icon = Icons.Filled.VisibilityOff,
                    label = stringResource(R.string.hide_balance),
                    detail = stringResource(R.string.hide_balance_description),
                    checked = state.hideCurrentBalance,
                    onCheckedChange = { onEvent(SettingsEvent.SetHideCurrentBalance(it)) },
                )
                Divider()
                SwitchRow(
                    icon = Icons.Filled.VisibilityOff,
                    label = stringResource(R.string.hide_income),
                    detail = stringResource(R.string.hide_income_description),
                    checked = state.hideIncome,
                    onCheckedChange = { onEvent(SettingsEvent.SetHideIncome(it)) },
                )
                Divider()
                SwitchRow(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    label = stringResource(R.string.transfers_as_income_expense),
                    detail = stringResource(R.string.transfers_as_income_expense_description),
                    checked = state.treatTransfersAsIncomeExpense,
                    onCheckedChange = { onEvent(SettingsEvent.SetTransfersAsIncomeExpense(it)) },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.Tune,
                    label = "Features",
                    detail = "SMS detection, keypad layout and more",
                    onClick = { nav.navigateTo(FeaturesScreen) },
                )
            }
        }

        item {
            SettingsSection("About") {
                RevampRow(
                    icon = Icons.Filled.Info,
                    label = "Version",
                    value = version,
                    onClick = { nav.navigateTo(ReleasesScreen) },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.Code,
                    label = stringResource(R.string.ivy_wallet_is_opensource),
                    onClick = onOpenRepo,
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.Groups,
                    label = "Contributors",
                    onClick = { nav.navigateTo(ContributorsScreen) },
                )
                Divider()
                RevampRow(
                    icon = Icons.Filled.Attribution,
                    label = "Attributions",
                    onClick = { nav.navigateTo(AttributionsScreen) },
                )
            }
        }

        item {
            DangerButton(
                modifier = Modifier.padding(horizontal = ScreenPadding),
                text = stringResource(R.string.delete_all_user_data),
                onClick = { deleteAllDataModalVisible = true },
            )
        }
    }

    CurrencyModal(
        title = stringResource(R.string.set_currency),
        initialCurrency = IvyCurrency.fromCode(state.currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        onEvent(SettingsEvent.SetCurrency(it))
    }

    NameModal(
        visible = nameModalVisible,
        name = state.name,
        dismiss = { nameModalVisible = false }
    ) {
        onEvent(SettingsEvent.SetName(it))
    }

    ChooseStartDateOfMonthModal(
        visible = chooseStartDateOfMonthVisible,
        selectedStartDateOfMonth = state.startDateOfMonth.toIntOrNull() ?: 1,
        dismiss = { chooseStartDateOfMonthVisible = false }
    ) {
        onEvent(SettingsEvent.SetStartDateOfMonth(it))
    }

    DeleteModal(
        title = stringResource(R.string.delete_all_user_data_question),
        description = stringResource(
            R.string.delete_all_user_data_warning,
            stringResource(R.string.your_account)
        ),
        visible = deleteAllDataModalVisible,
        dismiss = { deleteAllDataModalVisible = false },
        onDelete = {
            deleteAllDataModalVisible = false
            deleteAllDataModalFinalVisible = true
        }
    )

    DeleteModal(
        title = stringResource(
            R.string.confirm_all_userd_data_deletion,
            stringResource(R.string.all_of_your_data)
        ),
        description = stringResource(R.string.final_deletion_warning),
        visible = deleteAllDataModalFinalVisible,
        dismiss = { deleteAllDataModalFinalVisible = false },
        onDelete = {
            onEvent(SettingsEvent.DeleteAllUserData)
        }
    )

    ProgressModal(
        title = stringResource(R.string.exporting_data),
        description = stringResource(R.string.exporting_data_description),
        visible = state.progressState
    )
}

@Composable
private fun themeLabel(theme: Theme): String = when (theme) {
    Theme.LIGHT -> stringResource(R.string.light_mode)
    Theme.DARK -> stringResource(R.string.dark_mode)
    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
    Theme.AUTO -> stringResource(R.string.auto_mode)
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    RevampSection(title = title, modifier = Modifier.padding(horizontal = ScreenPadding), content = content)
}

@Composable
private fun Divider() {
    HorizontalDivider(color = revampColors().divider)
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    detail: String? = null,
) {
    val colors = revampColors()
    RevampRow(
        icon = icon,
        label = label,
        detail = detail,
        onClick = { onCheckedChange(!checked) },
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Composable
private fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = revampColors()
    Text(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.expenseTint)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        text = text,
        style = RevampType.bodyStrong,
        color = colors.expense,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(
            state = SettingsState(
                currencyCode = "INR",
                name = "",
                currentTheme = theme,
                lockApp = true,
                showNotifications = true,
                hideCurrentBalance = false,
                hideIncome = false,
                treatTransfersAsIncomeExpense = false,
                startDateOfMonth = "1",
                progressState = false,
                languageOptionVisible = false,
                autoBackup = AutoBackupViewState(
                    enabled = true,
                    hasFolder = true,
                    folder = "Documents/IvyBackups",
                    status = "Last backup: Today, 2:14 AM",
                    inProgress = false,
                ),
            ),
            version = "2025.07.17 (206)",
            onEvent = {},
            onBackupData = {},
            onExportToCSV = {},
            onSetAutoBackup = {},
            onPickBackupFolder = {},
            onOpenRepo = {},
        )
    }
}

/** For screenshot testing */
@Composable
fun SettingsUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}
