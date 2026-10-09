package com.ivy.smstransactions.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SmsReviewScreenImpl(
    viewModel: SmsReviewViewModel = screenScopedViewModel(),
) {
    SmsReviewUi(
        items = viewModel.uiState().items,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsReviewUi(
    items: ImmutableList<SmsReviewItem>,
    onEvent: (SmsReviewEvent) -> Unit,
) {
    val nav = navigation()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Bank SMS to review") },
                navigationIcon = {
                    IconButton(onClick = { nav.back() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id.value.toString() }) { item ->
                    SmsReviewCard(
                        item = item,
                        onAdd = { onEvent(SmsReviewEvent.Add(item.id)) },
                        onIgnore = { onEvent(SmsReviewEvent.Ignore(item.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsReviewCard(
    item: SmsReviewItem,
    onAdd: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor(isIncome = item.isIncome),
                )
            }
            Text(
                text = item.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.suggestion != null) {
                Text(
                    text = item.suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onAdd) {
                    Text(text = "Add")
                }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onIgnore) {
                    Text(text = "Ignore")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No bank SMS waiting. New transactions from your bank SMS will appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Green for income, red for expenses; lighter shades on a dark background so both stay readable. */
@Composable
private fun amountColor(isIncome: Boolean): Color {
    val darkBackground = MaterialTheme.colorScheme.surface.luminance() < DarkLuminance
    return when {
        isIncome && darkBackground -> IncomeOnDark
        isIncome -> IncomeOnLight
        darkBackground -> ExpenseOnDark
        else -> ExpenseOnLight
    }
}

private const val DarkLuminance = 0.5f
private val IncomeOnLight = Color(0xFF0B7A55)
private val IncomeOnDark = Color(0xFF5FD6A2)
private val ExpenseOnLight = Color(0xFFB42335)
private val ExpenseOnDark = Color(0xFFFF8A96)
