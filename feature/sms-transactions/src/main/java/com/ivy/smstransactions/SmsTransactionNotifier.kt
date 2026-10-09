package com.ivy.smstransactions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.domain.AppStarter
import com.ivy.smstransactions.store.QuickAdded
import com.ivy.smstransactions.store.RecordedSmsTransaction
import com.ivy.smstransactions.store.SmsSuggestion
import com.ivy.smstransactions.store.toPrefill
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class SmsTransactionNotifier @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val notificationService: NotificationService,
    private val appStarter: AppStarter,
) {

    fun show(recorded: RecordedSmsTransaction) {
        val trn = recorded.entity
        val suggestion = recorded.suggestion
        val notificationId = notificationId(trn.id)

        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.SMS_TRANSACTION,
                priority = NotificationCompat.PRIORITY_HIGH
            )
            .setContentTitle(
                listOfNotNull("${typeLabel(suggestion.type)} ${formatInr(trn.amount)}", trn.counterparty)
                    .joinToString(" · ")
            )
            .setContentText(contentText(trn, suggestion))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    addTransactionIntent(trn, suggestion),
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
                        .putExtra(DismissSmsTransactionReceiver.EXTRA_SMS_TRANSACTION_ID, trn.id.toString()),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        if (suggestion.isComplete) {
            notification.addAction(
                0,
                "Add",
                PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    Intent(context, QuickAddSmsTransactionReceiver::class.java)
                        .putExtra(QuickAddSmsTransactionReceiver.EXTRA_SMS_TRANSACTION_ID, trn.id.toString()),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        notificationService.showNotification(notification, notificationId)
    }

    /** Replaces the SMS notification with a confirmation; tapping it opens the saved transaction. */
    fun showSaved(added: QuickAdded) {
        val trn = added.sms
        val notificationId = notificationId(trn.id)
        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.SMS_TRANSACTION,
                priority = NotificationCompat.PRIORITY_LOW
            )
            .setContentTitle(listOfNotNull("Saved ${formatInr(trn.amount)}", trn.counterparty).joinToString(" · "))
            .setContentText((learned(added.suggestion) + "Tap to edit").joinToString(" · "))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    appStarter.getEditTransactionIntent(added.transaction.id.value, added.suggestion.type),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        notificationService.showNotification(notification, notificationId)
    }

    fun dismiss(smsTransactionId: UUID) {
        notificationService.dismissNotification(notificationId(smsTransactionId))
    }

    /** Opens "add transaction" pre-filled with the SMS details and the learned account/category. */
    fun addTransactionIntent(trn: SmsTransactionEntity, suggestion: SmsSuggestion): Intent =
        appStarter.getAddTransactionIntent(type = suggestion.type, prefill = trn.toPrefill(suggestion))

    private fun contentText(trn: SmsTransactionEntity, suggestion: SmsSuggestion): String {
        val source = listOfNotNull(trn.bank, trn.accountEnding?.let { "A/c XX$it" })
        return (learned(suggestion).ifEmpty { source } + "Tap to add").joinToString(" · ")
    }

    /** "HDFC Savings · Food & Drinks", or "HDFC Savings → ICICI Card" for a transfer. */
    private fun learned(suggestion: SmsSuggestion): List<String> =
        if (suggestion.type == TransactionType.TRANSFER) {
            val from = suggestion.accountName
            val to = suggestion.toAccountName
            listOfNotNull(
                when {
                    from != null && to != null -> "$from → $to"
                    to != null -> "To $to"
                    from != null -> "From $from"
                    else -> null
                }
            )
        } else {
            listOfNotNull(suggestion.accountName, suggestion.categoryName)
        }

    private fun typeLabel(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> "Income"
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.EXPENSE -> "Expense"
    }

    companion object {
        private val INR_FORMAT: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        fun formatInr(amount: Double): String = INR_FORMAT.format(amount)

        fun notificationId(smsTransactionId: UUID): Int = smsTransactionId.hashCode()
    }
}
