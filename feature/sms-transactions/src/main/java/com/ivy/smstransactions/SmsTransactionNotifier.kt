package com.ivy.smstransactions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.domain.AppStarter
import com.ivy.domain.TransactionPrefill
import com.ivy.smstransactions.store.RecordedSmsTransaction
import com.ivy.smstransactions.store.SmsSuggestion
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
                listOfNotNull("${trn.typeLabel()} ${formatInr(trn.amount)}", trn.counterparty)
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

        notificationService.showNotification(notification, notificationId)
    }

    fun dismiss(smsTransactionId: UUID) {
        notificationService.dismissNotification(notificationId(smsTransactionId))
    }

    /** Opens "add transaction" pre-filled with the SMS details and the learned account/category. */
    fun addTransactionIntent(trn: SmsTransactionEntity, suggestion: SmsSuggestion): Intent =
        appStarter.getAddTransactionIntent(
            type = trn.type,
            prefill = TransactionPrefill(
                amount = trn.amount,
                title = trn.counterparty,
                description = trn.toDescription(),
                dateTime = trn.dateTime,
                accountId = suggestion.accountId,
                categoryId = suggestion.categoryId,
                smsTransactionId = trn.id,
            )
        )

    private fun contentText(trn: SmsTransactionEntity, suggestion: SmsSuggestion): String {
        val learned = listOfNotNull(suggestion.accountName, suggestion.categoryName)
        val source = listOfNotNull(trn.bank, trn.accountEnding?.let { "A/c XX$it" })
        return (learned.ifEmpty { source } + "Tap to add").joinToString(" · ")
    }

    private fun SmsTransactionEntity.typeLabel(): String =
        if (type == TransactionType.INCOME) "Income" else "Expense"

    private fun SmsTransactionEntity.toDescription(): String = listOfNotNull(
        "Added from SMS",
        bank,
        accountEnding?.let { "A/c XX$it" },
        reference?.let { "Ref $it" },
    ).joinToString(" · ")

    companion object {
        private val INR_FORMAT: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        fun formatInr(amount: Double): String = INR_FORMAT.format(amount)

        fun notificationId(smsTransactionId: UUID): Int = smsTransactionId.hashCode()
    }
}
