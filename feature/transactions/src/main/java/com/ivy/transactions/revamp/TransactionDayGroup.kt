package com.ivy.transactions.revamp

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.revamp.RevampColors
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors

private const val AvatarTintAlpha = 0.18f

/** A day header ("Today", day total) followed by its transactions in one card. */
@Composable
fun TransactionDayGroup(
    group: TransactionDayGroupUi,
    onTransactionClick: (TransactionRowUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                modifier = Modifier.weight(1f),
                text = group.label,
                style = RevampType.caption,
                color = colors.inkMuted,
            )
            if (group.total != null) {
                Text(text = group.total, style = RevampType.caption, color = group.totalTone.color(colors))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .padding(horizontal = 14.dp)
        ) {
            group.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = colors.divider)
                TransactionRow(row = row, onClick = { onTransactionClick(row) })
            }
        }
    }
}

@Composable
fun TransactionRow(
    row: TransactionRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = revampColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    row.avatarColor?.let { Color(it).copy(alpha = AvatarTintAlpha) } ?: colors.primaryTint
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = row.initial, style = RevampType.bodyStrong, color = colors.ink)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = RevampType.bodyStrong,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = row.subtitle,
                    style = RevampType.label,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text = row.amount, style = RevampType.bodyStrong, color = row.tone.color(colors))
    }
}

fun AmountTone.color(colors: RevampColors): Color = when (this) {
    AmountTone.INCOME -> colors.income
    AmountTone.EXPENSE -> colors.expense
    AmountTone.NEUTRAL -> colors.ink
}
