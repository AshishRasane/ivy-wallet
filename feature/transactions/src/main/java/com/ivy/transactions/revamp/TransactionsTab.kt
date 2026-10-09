package com.ivy.transactions.revamp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import com.ivy.navigation.screenScopedViewModel

/** Space at the end of tab lists so the bottom bar never covers the last item. */
private val BottomBarClearance = 112.dp

@Composable
fun TransactionsTab(
    viewModel: TransactionsTabViewModel = screenScopedViewModel(),
) {
    TransactionsTabUi(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

@Composable
private fun TransactionsTabUi(
    state: TransactionsTabState,
    onEvent: (TransactionsTabEvent) -> Unit,
) {
    val colors = revampColors()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = BottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Transactions",
                    style = RevampType.headline,
                    color = colors.ink,
                )
                MonthSwitcher(
                    month = state.month,
                    onPrevious = { onEvent(TransactionsTabEvent.PreviousMonth) },
                    onNext = { onEvent(TransactionsTabEvent.NextMonth) },
                )
            }
        }
        item {
            SearchField(onClick = { onEvent(TransactionsTabEvent.Search) })
        }
        item {
            FilterChips(selected = state.filter, onSelect = { onEvent(TransactionsTabEvent.SelectFilter(it)) })
        }
        if (state.groups.isEmpty() && !state.loading) {
            item { EmptyMonth() }
        }
        items(state.groups, key = { it.label }) { group ->
            TransactionDayGroup(group = group, onTransactionClick = { onEvent(TransactionsTabEvent.Open(it)) })
        }
    }
}

@Composable
private fun MonthSwitcher(
    month: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = revampColors()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.ink)
        }
        Text(text = month, style = RevampType.body, color = colors.ink)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = colors.ink)
        }
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClickLabel = "Search transactions", onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = "Search title, category, note", style = RevampType.body, color = colors.inkMuted)
    }
}

@Composable
private fun FilterChips(
    selected: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit,
) {
    val colors = revampColors()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            TransactionFilter.ALL to "All",
            TransactionFilter.EXPENSES to "Expenses",
            TransactionFilter.INCOME to "Income",
        ).forEach { (filter, label) ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.ink else colors.surface)
                    .border(1.dp, if (isSelected) colors.ink else colors.border, CircleShape)
                    .clickable(role = Role.RadioButton) { onSelect(filter) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = RevampType.body,
                    color = if (isSelected) colors.ground else colors.ink,
                )
            }
        }
    }
}

@Composable
private fun EmptyMonth() {
    val colors = revampColors()
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        text = "No transactions this month.\nTap + to add one.",
        style = RevampType.body,
        color = colors.inkMuted,
        textAlign = TextAlign.Center,
    )
}
