package com.ivy.wallet

import android.content.Context
import android.content.Intent
import com.ivy.base.model.TransactionType
import com.ivy.domain.AppStarter
import com.ivy.domain.TransactionPrefill
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class IvyAppStarter @Inject constructor(
    @ApplicationContext
    private val context: Context
) : AppStarter {

    override fun getRootIntent(): Intent {
        return Intent(context, RootActivity::class.java)
    }

    override fun defaultStart() {
        context.startActivity(
            getRootIntent().apply {
                applyWidgetStartFlags()
            }
        )
    }

    override fun addTransactionStart(type: TransactionType) {
        context.startActivity(
            getRootIntent().apply {
                putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_TYPE, type)
                applyWidgetStartFlags()
            }
        )
    }

    override fun getAddTransactionIntent(
        type: TransactionType,
        prefill: TransactionPrefill,
    ): Intent {
        return getRootIntent().apply {
            putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_TYPE, type)
            prefill.amount?.let { putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_AMOUNT, it) }
            prefill.title?.let { putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_TITLE, it) }
            prefill.description?.let {
                putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_DESCRIPTION, it)
            }
            prefill.dateTime?.let {
                putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_DATE_TIME, it.toEpochMilli())
            }
            prefill.accountId?.let {
                putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_ACCOUNT_ID, it.toString())
            }
            prefill.categoryId?.let {
                putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_CATEGORY_ID, it.toString())
            }
            prefill.smsTransactionId?.let {
                putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_SMS_ID, it.toString())
            }
            applyWidgetStartFlags()
        }
    }

    override fun getEditTransactionIntent(
        transactionId: UUID,
        type: TransactionType,
    ): Intent {
        return getRootIntent().apply {
            putExtra(RootViewModel.EXTRA_ADD_TRANSACTION_TYPE, type)
            putExtra(RootViewModel.EXTRA_EDIT_TRANSACTION_ID, transactionId.toString())
            applyWidgetStartFlags()
        }
    }

    private fun Intent.applyWidgetStartFlags() {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
