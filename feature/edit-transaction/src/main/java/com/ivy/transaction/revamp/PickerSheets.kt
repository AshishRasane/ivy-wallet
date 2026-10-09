package com.ivy.transaction.revamp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ivy.design.revamp.RevampType
import com.ivy.design.revamp.revampColors
import kotlinx.collections.immutable.ImmutableList

/** One choice in a picker sheet: an account or a category. */
@Immutable
data class PickerOption(
    val key: String,
    val label: String,
    /** e.g. the account currency; null for none */
    val detail: String?,
    /** ARGB */
    val color: Int,
)

private const val DotTintAlpha = 0.22f

/**
 * A bottom sheet to pick an account or a category, with "＋ New …" at the end.
 * [noneLabel] adds an option to clear the choice (category is optional).
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    title: String,
    options: ImmutableList<PickerOption>,
    selectedKey: String?,
    newLabel: String,
    onSelect: (String?) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String? = null,
) {
    val colors = revampColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = title,
                style = RevampType.title,
                color = colors.ink,
            )
            LazyColumn(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (noneLabel != null) {
                    item {
                        OptionRow(
                            label = noneLabel,
                            detail = null,
                            dotColor = colors.border,
                            selected = selectedKey == null,
                            onClick = { onSelect(null) },
                        )
                    }
                }
                items(options, key = { it.key }) { option ->
                    OptionRow(
                        label = option.label,
                        detail = option.detail,
                        dotColor = Color(option.color),
                        selected = option.key == selectedKey,
                        onClick = { onSelect(option.key) },
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable(role = Role.Button, onClick = onNew)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.primaryTint),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.onPrimaryTint)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(text = newLabel, style = RevampType.bodyStrong, color = colors.onPrimaryTint)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    detail: String?,
    dotColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = revampColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.primaryTint else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = DotTintAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(modifier = Modifier.weight(1f), text = label, style = RevampType.body, color = colors.ink)
        if (detail != null) {
            Text(text = detail, style = RevampType.label, color = colors.inkMuted)
            Spacer(Modifier.width(8.dp))
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.onPrimaryTint)
        }
    }
}
