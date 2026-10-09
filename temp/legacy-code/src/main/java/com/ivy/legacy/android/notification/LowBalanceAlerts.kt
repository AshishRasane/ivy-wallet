@file:Suppress("InvalidPackageDeclaration") // next to NotificationService, in its package

package com.ivy.wallet.android.notification

import android.app.PendingIntent
import android.content.Context
import com.ivy.base.model.TransactionType
import com.ivy.data.datasource.LowBalanceAlertsDataSource
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.design.revamp.AmountFormat
import com.ivy.domain.AppStarter
import com.ivy.domain.TransactionPrefill
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifies when an account's balance drops below the amount the user set for it
 * (e.g. the account SIPs are paid from), once per drop: again only after it was topped up.
 */
@Singleton
class LowBalanceAlerts @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val settings: LowBalanceAlertsDataSource,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionDao: TransactionDao,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val notificationService: NotificationService,
    private val appStarter: AppStarter,
) {
    private val mutex = Mutex()

    /** Checks after every change to the transactions and to the alert settings, while the app runs. */
    @OptIn(FlowPreview::class)
    fun watch(scope: CoroutineScope) {
        scope.launch {
            combine(transactionDao.observeChanges(), settings.thresholds) { _, _ -> }
                .debounce(CHECK_DELAY_MS)
                .collect { check() }
        }
    }

    suspend fun check(): Unit = mutex.withLock {
        val thresholds = settings.thresholds.first()
        if (thresholds.isEmpty()) return@withLock
        val alerted = settings.alerted()
        val accounts = accountRepository.findAll().associateBy { it.id }
        thresholds.forEach { (id, threshold) ->
            val account = accounts[id] ?: return@forEach
            val balance = calcAccBalanceAct(CalcAccBalanceAct.Input(account = account)).balance.toDouble()
            when (decide(balance, threshold, alreadyAlerted = id in alerted)) {
                Decision.Notify -> {
                    notify(account, balance, threshold)
                    settings.setAlerted(id, true)
                }

                Decision.Clear -> {
                    notificationService.dismissNotification(notificationId(id))
                    settings.setAlerted(id, false)
                }

                Decision.None -> Unit
            }
        }
    }

    private suspend fun notify(account: Account, balance: Double, threshold: Double) {
        val currency = account.asset.code
        // tapping it opens a top-up: a transfer to this account from where it was last topped up
        val topUpFrom = transactionRepository.findAllTransfersToAccount(account.id)
            .maxByOrNull { it.time }
            ?.fromAccount?.value
        val intent = appStarter.getAddTransactionIntent(
            type = TransactionType.TRANSFER,
            prefill = TransactionPrefill(
                amount = null,
                title = null,
                description = null,
                dateTime = null,
                accountId = topUpFrom,
                toAccountId = account.id.value,
            ),
        )
        val notification = notificationService
            .defaultIvyNotification(channel = IvyNotificationChannel.LOW_BALANCE)
            .setContentTitle("${account.name.value} is low: ${AmountFormat.format(balance, currency)} left")
            .setContentText("Alert below ${AmountFormat.format(threshold, currency)} · Tap to top up")
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId(account.id),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        notificationService.showNotification(notification, notificationId(account.id))
    }

    private fun notificationId(accountId: AccountId): Int = ("low_balance" + accountId.value).hashCode()

    enum class Decision { Notify, Clear, None }

    companion object {
        private const val CHECK_DELAY_MS = 1_500L

        /** Alert once when the balance drops below [threshold]; reset when it's back at or above it. */
        fun decide(balance: Double, threshold: Double, alreadyAlerted: Boolean): Decision = when {
            balance < threshold && !alreadyAlerted -> Decision.Notify
            balance >= threshold && alreadyAlerted -> Decision.Clear
            else -> Decision.None
        }
    }
}
