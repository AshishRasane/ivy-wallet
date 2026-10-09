package com.ivy.smstransactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ivy.smstransactions.store.SmsTransactionStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Handles the "Ignore" action of an SMS transaction notification.
 */
@AndroidEntryPoint
class DismissSmsTransactionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var store: SmsTransactionStore

    @Inject
    lateinit var notifier: SmsTransactionNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_SMS_TRANSACTION_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return
        notifier.dismiss(id)

        val pendingResult = goAsync()
        scope.launch {
            try {
                store.markIgnored(id)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SMS_TRANSACTION_ID = "sms_transaction_id"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
