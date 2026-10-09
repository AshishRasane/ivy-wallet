package com.ivy.planned.auto

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Runs [PlannedAutoAddWorker] once a day, and now (when the app starts) to catch up. */
class PlannedAutoAddScheduler @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {
    fun schedule() {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PlannedAutoAddWorker>(1, TimeUnit.DAYS).build(),
        )
        workManager.enqueueUniqueWork(
            NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PlannedAutoAddWorker>().build(),
        )
    }

    private companion object {
        const val DAILY_WORK = "planned_auto_add_daily"
        const val NOW_WORK = "planned_auto_add_now"
    }
}
