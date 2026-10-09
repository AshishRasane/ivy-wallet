package com.ivy.wallet

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ivy.base.legacy.appContext
import com.ivy.planned.auto.PlannedAutoAddScheduler
import com.ivy.wallet.android.notification.LowBalanceAlerts
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * Created by iliyan on 24.02.18.
 */
@HiltAndroidApp
class IvyAndroidApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var plannedAutoAddScheduler: PlannedAutoAddScheduler

    @Inject
    lateinit var lowBalanceAlerts: LowBalanceAlerts

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appContext = this

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        // records planned payments marked "Add automatically" (e.g. daily SIPs) that became due
        plannedAutoAddScheduler.schedule()
        // "balance below X" alerts after every change while the app runs (and daily, see the worker)
        lowBalanceAlerts.watch(appScope)
    }
}
