package com.ivy.main.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.autobackup.AutoBackupSettings
import com.ivy.autobackup.AutoBackupState
import com.ivy.base.legacy.Theme
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.SmsDao
import com.ivy.data.db.entity.SmsTransactionStatus
import com.ivy.design.revamp.AmountFormat
import com.ivy.domain.features.Features
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import com.ivy.navigation.Navigation
import com.ivy.navigation.Screen
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.settings.SettingsAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import com.ivy.wallet.ui.theme.modal.BufferModalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Immutable
data class MoreState(
    val theme: String,
    val savingsGoal: String,
    val autoBackup: String,
    val smsDetection: String,
    val pendingSms: Int,
    val savingsGoalModal: BufferModalData?,
)

sealed interface MoreEvent {
    data class Open(val screen: Screen) : MoreEvent
    data object SwitchTheme : MoreEvent
    data object EditSavingsGoal : MoreEvent
    data class SetSavingsGoal(val amount: Double) : MoreEvent
    data object DismissSavingsGoal : MoreEvent
}

@Stable
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val nav: Navigation,
    private val ivyContext: IvyWalletCtx,
    private val settingsAct: SettingsAct,
    private val updateSettingsAct: UpdateSettingsAct,
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val autoBackupSettings: AutoBackupSettings,
    private val smsDao: SmsDao,
    private val features: Features,
    private val timeProvider: TimeProvider,
) : ComposeViewModel<MoreState, MoreEvent>() {

    private var theme by mutableStateOf<Theme?>(null)
    private var baseCurrency by mutableStateOf("")
    private var buffer by mutableDoubleStateOf(0.0)
    private var savingsGoalModal by mutableStateOf<BufferModalData?>(null)

    @Composable
    override fun uiState(): MoreState {
        LaunchedEffect(Unit) { load() }
        val backup = remember { autoBackupSettings.state }.collectAsState(initial = null).value
        val pending = remember { smsDao.observeCountByStatus(SmsTransactionStatus.PENDING) }
            .collectAsState(initial = 0).value
        return MoreState(
            theme = theme?.label().orEmpty(),
            savingsGoal = if (buffer > 0.0) AmountFormat.format(buffer, baseCurrency) else "Not set",
            autoBackup = backup?.status().orEmpty(),
            smsDetection = if (features.smsTransactionDetection.asEnabledState()) "On" else "Off",
            pendingSms = pending,
            savingsGoalModal = savingsGoalModal,
        )
    }

    override fun onEvent(event: MoreEvent) {
        when (event) {
            is MoreEvent.Open -> nav.navigateTo(event.screen)
            MoreEvent.SwitchTheme -> switchTheme()
            MoreEvent.EditSavingsGoal -> editSavingsGoal()
            is MoreEvent.SetSavingsGoal -> setSavingsGoal(event.amount)
            MoreEvent.DismissSavingsGoal -> savingsGoalModal = null
        }
    }

    private suspend fun load() {
        val settings = settingsAct(Unit)
        theme = settings.theme
        baseCurrency = settings.baseCurrency
        buffer = settings.bufferAmount.toDouble()
    }

    private fun switchTheme() {
        viewModelScope.launch {
            val next = settingsAct.getSettingsWithNextTheme()
            updateSettingsAct(next)
            ivyContext.switchTheme(next.theme)
            theme = next.theme
        }
    }

    private fun editSavingsGoal() {
        viewModelScope.launch {
            val balance = calcWalletBalanceAct(CalcWalletBalanceAct.Input(baseCurrency = baseCurrency))
            savingsGoalModal = BufferModalData(
                balance = balance.toDouble(),
                buffer = buffer,
                currency = baseCurrency,
            )
        }
    }

    private fun setSavingsGoal(amount: Double) {
        viewModelScope.launch {
            updateSettingsAct(settingsAct.getSettings().copy(bufferAmount = amount.toBigDecimal()))
            buffer = amount
        }
    }

    private fun Theme.label(): String = when (this) {
        Theme.LIGHT -> "Light"
        Theme.DARK -> "Dark"
        Theme.AMOLED_DARK -> "AMOLED"
        Theme.AUTO -> "System"
    }

    private fun AutoBackupState.status(): String {
        val lastSuccess = lastSuccessAt
        return when {
            !enabled -> "Off"
            lastFailure != null -> "Failed"
            lastSuccess != null -> formatTime(lastSuccess)
            else -> "On"
        }
    }

    private fun formatTime(time: Instant): String {
        val local = time.atZone(timeProvider.getZoneId()).toLocalDateTime()
        val day = when (local.toLocalDate()) {
            timeProvider.localDateNow() -> "Today"
            timeProvider.localDateNow().minusDays(1) -> "Yesterday"
            else -> local.format(DateTimeFormatter.ofPattern("d MMM"))
        }
        return "$day, ${local.format(DateTimeFormatter.ofPattern("h:mm a"))}"
    }
}
