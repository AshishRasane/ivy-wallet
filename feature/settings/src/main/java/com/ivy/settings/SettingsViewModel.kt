package com.ivy.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ivy.autobackup.AutoBackupFiles
import com.ivy.autobackup.AutoBackupNotifier
import com.ivy.autobackup.AutoBackupResult
import com.ivy.autobackup.AutoBackupRunner
import com.ivy.autobackup.AutoBackupScheduler
import com.ivy.autobackup.AutoBackupSettings
import com.ivy.autobackup.AutoBackupState
import com.ivy.autobackup.message
import com.ivy.base.Toaster
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeProvider
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.refreshWidget
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.data.db.dao.write.WriteSettingsDao
import com.ivy.data.model.primitive.AssetCode
import com.ivy.domain.RootScreen
import com.ivy.domain.usecase.csv.ExportCsvUseCase
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.frp.monad.Res
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.LogoutLogic
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import com.ivy.legacy.utils.getISOFormattedDateTime
import com.ivy.legacy.utils.ioThread
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.legacy.utils.uiThread
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import com.ivy.wallet.domain.action.global.UpdateStartDayOfMonthAct
import com.ivy.wallet.domain.action.settings.SettingsAct
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val ivyContext: IvyWalletCtx,
    private val logoutLogic: LogoutLogic,
    private val sharedPrefs: SharedPrefs,
    private val backupDataUseCase: BackupDataUseCase,
    private val startDayOfMonthAct: StartDayOfMonthAct,
    private val updateStartDayOfMonthAct: UpdateStartDayOfMonthAct,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val settingsAct: SettingsAct,
    private val updateSettingsAct: UpdateSettingsAct,
    private val settingsWriter: WriteSettingsDao,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val autoBackupSettings: AutoBackupSettings,
    private val autoBackupRunner: AutoBackupRunner,
    private val autoBackupScheduler: AutoBackupScheduler,
    private val autoBackupNotifier: AutoBackupNotifier,
    private val toaster: Toaster,
    private val timeProvider: TimeProvider,
    @ApplicationContext private val context: Context
) : ComposeViewModel<SettingsState, SettingsEvent>() {

    private val currencyCode = mutableStateOf("")
    private val name = mutableStateOf("")
    private val currentTheme = mutableStateOf<Theme>(Theme.AUTO)
    private val lockApp = mutableStateOf(false)
    private val showNotifications = mutableStateOf(true)
    private val hideCurrentBalance = mutableStateOf(false)
    private val hideIncome = mutableStateOf(false)
    private val treatTransfersAsIncomeExpense = mutableStateOf(false)
    private val startDateOfMonth = mutableIntStateOf(1)
    private val progressState = mutableStateOf(false)
    private val autoBackupInProgress = mutableStateOf(false)

    @Composable
    override fun uiState(): SettingsState {
        LaunchedEffect(Unit) {
            onStart()
        }

        return SettingsState(
            currencyCode = getCurrencyCode(),
            name = getName(),
            currentTheme = getCurrentTheme(),
            lockApp = getLockApp(),
            showNotifications = getShowNotifications(),
            hideCurrentBalance = getHideCurrentBalance(),
            treatTransfersAsIncomeExpense = getTreatTransfersAsIncomeExpense(),
            startDateOfMonth = getStartDateOfMonth(),
            progressState = getProgressState(),
            hideIncome = getHideIncome(),
            languageOptionVisible = isLanguageOptionVisible(),
            autoBackup = getAutoBackup(),
        )
    }

    private suspend fun onStart() {
        initializeCurrency()
        initializeName()
        initializeCurrentTheme()
        initializeLockApp()
        initializeShowNotifications()
        initializeHideCurrentBalance()
        initializeHideIncome()
        initializeTransfersAsIncomeExpense()
        initializeStartDateOfMonth()
    }

    private suspend fun initializeCurrency() {
        val settings = ioThread {
            settingsDao.findFirst()
        }

        currencyCode.value = settings.currency
    }

    private suspend fun initializeName() {
        val settings = ioThread {
            settingsDao.findFirst()
        }

        name.value = settings.name
    }

    private suspend fun initializeCurrentTheme() {
        currentTheme.value = settingsAct(Unit).theme
    }

    private fun initializeLockApp() {
        lockApp.value = sharedPrefs.getBoolean(SharedPrefs.APP_LOCK_ENABLED, false)
    }

    private fun initializeShowNotifications() {
        showNotifications.value = sharedPrefs.getBoolean(
            SharedPrefs.SHOW_NOTIFICATIONS, true
        )
    }

    private fun initializeHideCurrentBalance() {
        hideCurrentBalance.value =
            sharedPrefs.getBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, false)
    }

    private fun initializeHideIncome() {
        hideIncome.value =
            sharedPrefs.getBoolean(SharedPrefs.HIDE_INCOME, false)
    }

    private fun initializeTransfersAsIncomeExpense() {
        treatTransfersAsIncomeExpense.value =
            sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false)
    }

    private suspend fun initializeStartDateOfMonth() {
        startDateOfMonth.intValue = startDayOfMonthAct(Unit)
    }

    @Composable
    private fun getCurrencyCode(): String {
        return currencyCode.value
    }

    @Composable
    private fun getName(): String {
        return name.value
    }

    @Composable
    private fun getCurrentTheme(): Theme {
        return currentTheme.value
    }

    @Composable
    private fun getLockApp(): Boolean {
        return lockApp.value
    }

    @Composable
    private fun getShowNotifications(): Boolean {
        return showNotifications.value
    }

    @Composable
    private fun getHideCurrentBalance(): Boolean {
        return hideCurrentBalance.value
    }

    @Composable
    private fun getHideIncome(): Boolean {
        return hideIncome.value
    }

    @Composable
    private fun getTreatTransfersAsIncomeExpense(): Boolean {
        return treatTransfersAsIncomeExpense.value
    }

    @Composable
    private fun getStartDateOfMonth(): String {
        return startDateOfMonth.intValue.toString()
    }

    @Composable
    private fun getProgressState(): Boolean {
        return progressState.value
    }

    private fun isLanguageOptionVisible(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetCurrency -> setCurrency(event.newCurrency)
            is SettingsEvent.SetName -> setName(event.newName)
            is SettingsEvent.ExportToCsv -> exportToCSV(event.rootScreen)
            is SettingsEvent.BackupData -> exportToZip(event.rootScreen)
            SettingsEvent.SwitchTheme -> switchTheme()
            is SettingsEvent.SetLockApp -> setLockApp(event.lockApp)
            is SettingsEvent.SetShowNotifications -> setShowNotifications(event.showNotifications)
            is SettingsEvent.SetHideCurrentBalance -> setHideCurrentBalance(
                event.hideCurrentBalance
            )

            is SettingsEvent.SetHideIncome -> setHideIncome(
                event.hideIncome
            )

            is SettingsEvent.SetTransfersAsIncomeExpense -> setTransfersAsIncomeExpense(
                event.treatTransfersAsIncomeExpense
            )

            is SettingsEvent.SetStartDateOfMonth -> setStartDateOfMonth(event.startDate)

            SettingsEvent.DeleteCloudUserData -> deleteCloudUserData()
            SettingsEvent.DeleteAllUserData -> deleteAllUserData()
            SettingsEvent.SwitchLanguage -> switchLanguage()
            is SettingsEvent.SetAutoBackup -> setAutoBackup(event.enabled)
            is SettingsEvent.AutoBackupFolderSelected -> onAutoBackupFolderSelected(event.folderUri)
            SettingsEvent.BackupNow -> runAutoBackup(force = true)
        }
    }

    @Composable
    private fun getAutoBackup(): AutoBackupViewState {
        val state = remember { autoBackupSettings.state }.collectAsState(initial = null).value
        return AutoBackupViewState(
            enabled = state?.enabled ?: false,
            hasFolder = state?.folderUri != null,
            folder = state?.folderUri?.let(::folderDisplayName),
            status = state?.let(::autoBackupStatus).orEmpty(),
            inProgress = autoBackupInProgress.value,
        )
    }

    private fun folderDisplayName(folderUri: String): String? = runCatching {
        AutoBackupFiles.folderDisplayName(DocumentsContract.getTreeDocumentId(Uri.parse(folderUri)))
    }.getOrNull()

    private fun autoBackupStatus(state: AutoBackupState): String {
        val failure = state.lastFailure
        val lastSuccess = state.lastSuccessAt
        return when {
            failure != null -> "Last backup failed: ${failure.message()}"
            lastSuccess != null -> "Last backup: ${formatBackupTime(lastSuccess)}"
            else -> "No backup yet"
        }
    }

    private fun formatBackupTime(time: Instant): String {
        val local = time.atZone(timeProvider.getZoneId()).toLocalDateTime()
        val today = timeProvider.localDateNow()
        val day = when (local.toLocalDate()) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> local.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
        return "$day, ${local.format(DateTimeFormatter.ofPattern("h:mm a"))}"
    }

    private fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            autoBackupSettings.setEnabled(enabled)
            if (enabled) {
                autoBackupScheduler.schedule()
                runAutoBackupInternal(force = false)
            } else {
                autoBackupScheduler.cancel()
                autoBackupNotifier.dismissFailure()
            }
        }
    }

    private fun onAutoBackupFolderSelected(folderUri: Uri) {
        viewModelScope.launch {
            val previous = autoBackupSettings.current().folderUri?.let(Uri::parse)
            val accessGranted = runCatching {
                autoBackupRunner.takeFolderAccess(folderUri, previous)
            }.isSuccess
            if (!accessGranted) {
                toaster.show("Couldn't get access to that folder. Please choose another one.")
                return@launch
            }
            autoBackupSettings.setFolder(folderUri.toString())
            autoBackupSettings.setEnabled(true)
            autoBackupScheduler.schedule()
            runAutoBackupInternal(force = true)
        }
    }

    private fun runAutoBackup(force: Boolean) {
        viewModelScope.launch { runAutoBackupInternal(force) }
    }

    private suspend fun runAutoBackupInternal(force: Boolean) {
        if (autoBackupInProgress.value) return
        autoBackupInProgress.value = true
        val result = autoBackupRunner.backup(force)
        autoBackupInProgress.value = false

        if (result !is AutoBackupResult.Failed) autoBackupNotifier.dismissFailure()
        toaster.show(
            when (result) {
                is AutoBackupResult.Saved -> "Backup saved: ${result.fileName}"
                AutoBackupResult.Unchanged -> "Backup is up to date - no changes since the last one"
                is AutoBackupResult.Failed -> "Backup failed: ${result.failure.message()}"
            }
        )
    }

    private fun setCurrency(newCurrency: String) {
        currencyCode.value = newCurrency

        viewModelScope.launch {
            ioThread {
                settingsWriter.save(
                    settingsDao.findFirst().copy(
                        currency = newCurrency
                    )
                )
                AssetCode.from(newCurrency).onRight {
                    syncExchangeRatesUseCase.sync(it)
                }
            }
        }
    }

    private fun setName(newName: String) {
        name.value = newName

        viewModelScope.launch {
            ioThread {
                settingsWriter.save(
                    settingsDao.findFirst().copy(
                        name = newName
                    )
                )
            }
        }
    }

    private fun exportToCSV(rootScreen: RootScreen) {
        ivyContext.createNewFile(
            "IvyWalletExport_${
                timeNowUTC().getISOFormattedDateTime()
            }.csv"
        ) { fileUri ->
            viewModelScope.launch {
                exportCsvUseCase.exportToFile(
                    outputFile = fileUri
                )

                rootScreen.shareCSVFile(
                    fileUri = fileUri
                )
            }
        }
    }

    private fun exportToZip(rootScreen: RootScreen) {
        ivyContext.createNewFile(
            "IvyWalletBackup_${
                timeNowUTC().getISOFormattedDateTime()
            }.zip"
        ) { fileUri ->
            viewModelScope.launch(Dispatchers.IO) {
                progressState.value = true
                backupDataUseCase.exportToFile(zipFileUri = fileUri)
                progressState.value = false

                sharedPrefs.putBoolean(SharedPrefs.DATA_BACKUP_COMPLETED, true)
                ivyContext.dataBackupCompleted = true

                uiThread {
                    rootScreen.shareZipFile(
                        fileUri = fileUri
                    )
                }
            }
        }
    }

    private fun switchTheme() {
        viewModelScope.launch {
            settingsAct.getSettingsWithNextTheme().run {
                updateSettingsAct(this)
                ivyContext.switchTheme(this.theme)
                currentTheme.value = this.theme
            }
        }
    }

    private fun setLockApp(lock: Boolean) {
        lockApp.value = lock

        viewModelScope.launch {
            sharedPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, lock)
            refreshWidget(WalletBalanceWidgetReceiver::class.java)
        }
    }

    private fun setShowNotifications(notificationsShow: Boolean) {
        showNotifications.value = notificationsShow

        viewModelScope.launch {
            sharedPrefs.putBoolean(SharedPrefs.SHOW_NOTIFICATIONS, notificationsShow)
        }
    }

    private fun setHideCurrentBalance(hideBalance: Boolean) {
        hideCurrentBalance.value = hideBalance

        viewModelScope.launch {
            sharedPrefs.putBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, hideBalance)
        }
    }

    private fun setHideIncome(isHideIncome: Boolean) {
        hideIncome.value = isHideIncome

        viewModelScope.launch {
            sharedPrefs.putBoolean(SharedPrefs.HIDE_INCOME, isHideIncome)
        }
    }

    private fun setTransfersAsIncomeExpense(setTransfersAsIncomeExpense: Boolean) {
        treatTransfersAsIncomeExpense.value = setTransfersAsIncomeExpense

        viewModelScope.launch {
            sharedPrefs.putBoolean(
                SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE,
                treatTransfersAsIncomeExpense.value
            )
        }
    }

    private fun setStartDateOfMonth(startDate: Int) {
        viewModelScope.launch {
            when (val res = updateStartDayOfMonthAct(startDate)) {
                is Res.Err -> {}
                is Res.Ok -> {
                    startDateOfMonth.intValue = res.data
                }
            }
        }
    }

    private fun deleteCloudUserData() {
        viewModelScope.launch {
            cloudLogout()
        }
    }

    private fun cloudLogout() {
        viewModelScope.launch {
            logoutLogic.cloudLogout()
        }
    }

    private fun deleteAllUserData() {
        viewModelScope.launch {
            logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutLogic.logout()
        }
    }

    private fun switchLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.applicationContext.startActivity(intent)
        }
    }
}
