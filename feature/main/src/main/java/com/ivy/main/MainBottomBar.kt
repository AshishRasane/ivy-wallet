package com.ivy.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.legacy.data.model.MainTab

/**
 * Bottom navigation: Home · Transactions · ＋ · Reports · More. Labels are always visible
 * and ＋ always adds a transaction.
 */
@Composable
fun MainBottomBar(
    tab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onAdd: () -> Unit,
    onReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(modifier = modifier.background(colors.surface)) {
        HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarItem(Icons.Filled.Home, "Home", selected = tab == MainTab.HOME) { onSelectTab(MainTab.HOME) }
            BarItem(Icons.AutoMirrored.Filled.ReceiptLong, "Transactions", selected = tab == MainTab.TRANSACTIONS) {
                onSelectTab(MainTab.TRANSACTIONS)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AddButton(onClick = onAdd)
            }
            BarItem(Icons.AutoMirrored.Filled.ShowChart, "Reports", selected = false, onClick = onReports)
            BarItem(Icons.Filled.GridView, "More", selected = tab == MainTab.MORE) { onSelectTab(MainTab.MORE) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    val color = if (selected) colors.primary else colors.inkMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = RevampType.caption, color = color, maxLines = 1)
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val colors = revampColors()
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = colors.primary, spotColor = colors.primary)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Add transaction" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(28.dp))
    }
}

/** What ＋ can add. Replaced by a single add screen in a later revamp step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
    onPlannedPayment: () -> Unit,
) {
    val colors = revampColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Add", style = RevampType.title, color = colors.ink)
            Spacer(Modifier.height(4.dp))
            SheetOption(Icons.Filled.ArrowUpward, "Expense", colors.expense, colors.expenseTint, onExpense)
            SheetOption(Icons.Filled.ArrowDownward, "Income", colors.income, colors.incomeTint, onIncome)
            SheetOption(Icons.Filled.SwapHoriz, "Transfer", colors.onPrimaryTint, colors.primaryTint, onTransfer)
            SheetOption(
                Icons.Filled.EventRepeat,
                "Planned payment",
                colors.onPrimaryTint,
                colors.primaryTint,
                onPlannedPayment,
            )
        }
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text = label, style = RevampType.bodyStrong, color = colors.ink)
    }
}
