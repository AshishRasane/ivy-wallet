package com.ivy.smstransactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ivy.domain.features.Features
import com.ivy.smstransactions.parser.BankSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * Listens for incoming SMS and, when the "Detect transactions from bank SMS" feature is on,
 * shows a notification for every detected income/expense.
 * Only the incoming message is processed - the inbox is never read and nothing is stored.
 */
@AndroidEntryPoint
class SmsTransactionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var parser: BankSmsParser

    @Inject
    lateinit var notifier: SmsTransactionNotifier

    @Inject
    lateinit var features: Features

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty()
        if (messages.isEmpty()) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                if (!features.smsTransactionDetection.isEnabled(context.applicationContext)) return@launch

                // A long SMS arrives as several parts - join them per sender.
                messages.groupBy { it.originatingAddress.orEmpty() }
                    .forEach { (sender, parts) ->
                        val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                        val receivedAt = Instant.ofEpochMilli(parts.first().timestampMillis)
                        parser.parse(sender, body).fold(
                            ifLeft = { reason -> Timber.d("SMS from $sender skipped: $reason") },
                            ifRight = { trn -> notifier.show(trn, sender, body, receivedAt) },
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to process SMS")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
