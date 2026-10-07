package com.ivy.smstransactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Handles the "Ignore" action of an SMS transaction notification.
 */
class DismissSmsTransactionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
