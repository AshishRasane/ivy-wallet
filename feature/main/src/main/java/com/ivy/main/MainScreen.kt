package com.ivy.main

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ivy.main.more.MoreTab
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.Screen
import com.ivy.transactions.revamp.TransactionsTab
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.model.TransactionType
import com.ivy.home.HomeTab
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.EditPlannedScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.navigation

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.MainScreen(screen: MainScreen) {
    val viewModel: MainViewModel = viewModel()

    onScreenStart {
        viewModel.start(screen)
    }

    val ivyContext = ivyWalletCtx()
    UI(
        tab = ivyContext.mainTab,
        selectTab = viewModel::selectTab,
    )
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,
) {
    when (tab) {
        MainTab.HOME -> HomeTab()
        MainTab.TRANSACTIONS -> TransactionsTab()
        MainTab.MORE -> MoreTab()
    }

    var addSheetVisible by remember { mutableStateOf(false) }
    val nav = navigation()
    MainBottomBar(
        modifier = Modifier.align(Alignment.BottomCenter),
        tab = tab,
        onSelectTab = selectTab,
        onAdd = { addSheetVisible = true },
        onReports = { nav.navigateTo(ReportScreen) },
    )

    if (addSheetVisible) {
        val add = { screen: Screen ->
            addSheetVisible = false
            nav.navigateTo(screen)
        }
        AddTransactionSheet(
            onDismiss = { addSheetVisible = false },
            onExpense = { add(EditTransactionScreen(initialTransactionId = null, type = TransactionType.EXPENSE)) },
            onIncome = { add(EditTransactionScreen(initialTransactionId = null, type = TransactionType.INCOME)) },
            onTransfer = { add(EditTransactionScreen(initialTransactionId = null, type = TransactionType.TRANSFER)) },
            onPlannedPayment = {
                add(EditPlannedScreen(type = TransactionType.EXPENSE, plannedPaymentRuleId = null))
            },
        )
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun PreviewMainScreen() {
    IvyWalletPreview {
        UI(
            tab = MainTab.HOME,
            selectTab = {},
        )
    }
}
