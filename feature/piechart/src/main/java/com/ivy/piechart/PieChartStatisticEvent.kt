package com.ivy.piechart

import com.ivy.base.model.TransactionType

import com.ivy.data.model.Category
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.navigation.PieChartStatisticScreen

sealed interface PieChartStatisticEvent {
    data class OnStart(val screen: PieChartStatisticScreen) : PieChartStatisticEvent
    data object OnSelectNextMonth : PieChartStatisticEvent
    data object OnSelectPreviousMonth : PieChartStatisticEvent
    data class OnSetPeriod(val timePeriod: TimePeriod) : PieChartStatisticEvent
    data class OnCategoryClicked(val category: Category?) : PieChartStatisticEvent
    data class OnTypeChanged(val type: TransactionType) : PieChartStatisticEvent
    data object OnInvestedSelected : PieChartStatisticEvent
    data class OnShowMonthModal(val timePeriod: TimePeriod?) : PieChartStatisticEvent
}
