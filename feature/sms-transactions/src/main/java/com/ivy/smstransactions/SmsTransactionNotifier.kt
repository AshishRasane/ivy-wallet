package com.ivy.smstransactions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ivy.base.model.TransactionType
import com.ivy.domain.AppStarter
import com.ivy.domain.TransactionPrefill
import com.ivy.smstransactions.parser.SmsTransaction
import com.ivy.smstransactions.parser.resolveDateTime
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

class SmsTransactionNotifier @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val notificationService: NotificationService,
    private val appStarter: AppStarter,
) {

    fun show(trn: SmsTransaction, sender: String, body: String, receivedAt: Instant) {
        // The same SMS (or a duplicate alert with the same ref) maps to the same notification.
        val notificationId = (trn.reference ?: "$sender|$body").hashCode()
        val typeLabel = if (trn.type == TransactionType.INCOME) "Income" else "Expense"
        val amount = INR_FORMAT.format(trn.amount)

        val addIntent = appStarter.getAddTransactionIntent(
            type = trn.type,
            prefill = TransactionPrefill(
                amount = trn.amount,
                title = trn.counterparty,
                description = trn.toDescription(),
                dateTime = trn.resolveDateTime(receivedAt, ZoneId.systemDefault()),
            )
        )

        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.SMS_TRANSACTION,
                priority = NotificationCompat.PRIORITY_HIGH
            )
            .setContentTitle(listOfNotNull("$typeLabel $amount", trn.counterparty).joinToString(" · "))
            .setContentText(
                listOfNotNull(trn.bank, trn.accountEnding?.let { "A/c XX$it" }, "Tap to add")
                    .joinToString(" · ")
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                "Ignore",
                PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    Intent(context, DismissSmsTransactionReceiver::class.java)
                        .putExtra(DismissSmsTransactionReceiver.EXTRA_NOTIFICATION_ID, notificationId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        notificationService.showNotification(notification, notificationId)
    }

    private fun SmsTransaction.toDescription(): String = listOfNotNull(
        "Added from SMS",
        bank,
        accountEnding?.let { "A/c XX$it" },
        reference?.let { "Ref $it" },
    ).joinToString(" · ")

    companion object {
        private val INR_FORMAT: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }
}
