package com.ivy.piechart

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID

@Immutable
data class PieChartStatisticState(
    val transactionType: TransactionType,
    val period: TimePeriod,
    val baseCurrency: String,
    val totalAmount: Double,
    val categoryAmounts: ImmutableList<CategoryAmount>,
    val selectedCategory: SelectedCategory?,
    val accountIdFilterList: ImmutableList<UUID>,
    val showCloseButtonOnly: Boolean,
    val filterExcluded: Boolean,
    val transactions: ImmutableList<Transaction>,
    val choosePeriodModal: ChoosePeriodModalData?,
    /** Monthly totals up to the selected month; empty when the period isn't a month. */
    val trend: ImmutableList<TrendBar>,
    /** "5-month avg ₹31.2k" */
    val trendAverage: String?,
    /** "12% less than September" */
    val comparison: String?,
    val transactionCount: Int,
)
