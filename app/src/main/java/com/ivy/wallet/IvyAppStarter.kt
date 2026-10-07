package com.ivy.wallet

import android.content.Context
import android.content.Intent
import com.ivy.base.model.TransactionType
import com.ivy.domain.AppStarter
import com.ivy.domain.TransactionPrefill
import dagger.hilt.android.qualifiers.ApplicationContext
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
            applyWidgetStartFlags()
        }
    }

    private fun Intent.applyWidgetStartFlags() {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
