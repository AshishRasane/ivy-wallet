package com.ivy.design.revamp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Colors of the revamped UI. Green and red are only used for income and expenses;
 * [primary] is for actions. Every text/background pair meets WCAG AA (4.5:1).
 */
@Immutable
data class RevampColors(
    /** Screen background. */
    val ground: Color,
    /** Cards, sheets, bottom bar. */
    val surface: Color,
    val border: Color,
    val divider: Color,
    /** Main text. */
    val ink: Color,
    /** Secondary text (labels, captions). */
    val inkMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    /** Soft action background (banners, selected chips). */
    val primaryTint: Color,
    val onPrimaryTint: Color,
    val income: Color,
    val incomeTint: Color,
    val expense: Color,
    val expenseTint: Color,
)

/** Light palette; also used by the old design system so old screens match. */
val RevampLight = RevampColors(
    ground = Color(0xFFF4F4F8),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE4E4EC),
    divider = Color(0xFFEEEEF3),
    ink = Color(0xFF15151C),
    inkMuted = Color(0xFF5D5E6B),
    primary = Color(0xFF5B3CF5),
    onPrimary = Color(0xFFFFFFFF),
    primaryTint = Color(0xFFECE8FF),
    onPrimaryTint = Color(0xFF2E1A9E),
    income = Color(0xFF0B7A55),
    incomeTint = Color(0xFFDDF4EA),
    expense = Color(0xFFB42335),
    expenseTint = Color(0xFFFBE5E8),
)

val RevampDark = RevampColors(
    ground = Color(0xFF0F0F14),
    surface = Color(0xFF1A1A22),
    border = Color(0xFF2C2C38),
    divider = Color(0xFF24242E),
    ink = Color(0xFFF2F2F7),
    inkMuted = Color(0xFFA6A7B5),
    primary = Color(0xFF7B61FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryTint = Color(0xFF2A2350),
    onPrimaryTint = Color(0xFFD4CCFF),
    income = Color(0xFF5FD6A2),
    incomeTint = Color(0xFF133628),
    expense = Color(0xFFFF8A96),
    expenseTint = Color(0xFF3D1A20),
)

val RevampAmoled = RevampDark.copy(
    ground = Color(0xFF000000),
    surface = Color(0xFF121217),
    divider = Color(0xFF1E1E26),
)

private const val DarkLuminance = 0.5f
private const val TrueBlackLuminance = 0.001f

/**
 * Follows the app's theme (light / dark / AMOLED), which is applied to [MaterialTheme].
 */
@Composable
fun revampColors(): RevampColors {
    val background = MaterialTheme.colorScheme.background.luminance()
    return when {
        background < TrueBlackLuminance -> RevampAmoled
        background < DarkLuminance -> RevampDark
        else -> RevampLight
    }
}
