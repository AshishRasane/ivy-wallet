package com.ivy.smstransactions.store

import com.ivy.domain.SmsTransactionCallbacks
import com.ivy.smstransactions.SmsTransactionNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Inject

class SmsTransactionCallbacksImpl @Inject constructor(
    private val store: SmsTransactionStore,
    private val notifier: SmsTransactionNotifier,
) : SmsTransactionCallbacks {
    override suspend fun onSaved(smsTransactionId: UUID, accountId: UUID, categoryId: UUID?, toAccountId: UUID?) {
        store.markAdded(smsTransactionId, accountId, categoryId, toAccountId)
        notifier.dismiss(smsTransactionId)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface SmsTransactionsModule {
    @Binds
    fun smsTransactionCallbacks(impl: SmsTransactionCallbacksImpl): SmsTransactionCallbacks
}
