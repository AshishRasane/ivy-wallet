package com.ivy.planned.auto

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivy.base.legacy.refreshWidget
import com.ivy.wallet.domain.deprecated.logic.PlannedAutoAdd
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Records the planned payments marked "Add automatically" whose dates have passed.
 *
 * Dependencies come from a Hilt [EntryPoint], so the worker only needs the default
 * (Context, WorkerParameters) constructor that WorkManager can always instantiate.
 */
class PlannedAutoAddWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun plannedAutoAdd(): PlannedAutoAdd
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        if (deps.plannedAutoAdd().run() > 0) {
            refreshWidget(WalletBalanceWidgetReceiver::class.java)
        }
        return Result.success()
    }
}
