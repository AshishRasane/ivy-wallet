package com.ivy.autobackup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Daily automatic backup.
 *
 * Dependencies come from a Hilt [EntryPoint], so the worker only needs the default
 * (Context, WorkerParameters) constructor that WorkManager can always instantiate.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun settings(): AutoBackupSettings
        fun runner(): AutoBackupRunner
        fun notifier(): AutoBackupNotifier
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        if (!deps.settings().current().enabled) return Result.success()

        when (val result = deps.runner().backup(force = false)) {
            is AutoBackupResult.Failed -> deps.notifier().showFailure(result.failure)
            else -> deps.notifier().dismissFailure()
        }
        // A failure is reported to the user; retrying immediately wouldn't fix a missing folder.
        return Result.success()
    }
}
