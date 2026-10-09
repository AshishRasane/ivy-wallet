package com.ivy.smstransactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ivy.smstransactions.store.SmsQuickAdd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Handles the "Add" action of an SMS transaction notification: saves it without opening the app.
 */
@AndroidEntryPoint
class QuickAddSmsTransactionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var quickAdd: SmsQuickAdd

    @Inject
    lateinit var notifier: SmsTransactionNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_SMS_TRANSACTION_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return

        val pendingResult = goAsync()
        scope.launch {
            try {
                quickAdd.add(id).fold(
                    // the original notification stays, so the user can still tap it to add manually
                    ifLeft = { reason -> Timber.w("Quick add of SMS transaction failed: $reason") },
                    ifRight = { added -> notifier.showSaved(added) },
                )
            } catch (e: Exception) {
                Timber.e(e, "Quick add of SMS transaction failed")
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
