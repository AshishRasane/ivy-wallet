package com.ivy.autobackup

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ivy.domain.AppStarter
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AutoBackupNotifier @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val notificationService: NotificationService,
    private val appStarter: AppStarter,
) {
    fun showFailure(failure: AutoBackupFailure) {
        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.AUTO_BACKUP,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentTitle("Automatic backup failed")
            .setContentText("${failure.message()} Open Settings → Automatic backup to fix it.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${failure.message()} Open Settings → Automatic backup to fix it.")
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID,
                    appStarter.getRootIntent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        notificationService.showNotification(notification, NOTIFICATION_ID)
    }

    fun dismissFailure() {
        notificationService.dismissNotification(NOTIFICATION_ID)
    }

    private companion object {
        const val NOTIFICATION_ID = 0x1B4C
    }
}

fun AutoBackupFailure.message(): String = when (this) {
    AutoBackupFailure.NoFolder -> "No backup folder is selected."
    AutoBackupFailure.FolderUnavailable -> "The backup folder was deleted or is no longer accessible."
    AutoBackupFailure.WriteFailed -> "The backup file couldn't be written."
}
