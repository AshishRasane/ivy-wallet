package com.ivy.design.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ivy.design.revamp.RevampAmoled
import com.ivy.design.revamp.RevampDark
import com.ivy.design.revamp.RevampLight
import com.ivy.design.system.colors.IvyColors

@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) ivyDarkColorScheme(isTrueBlack) else ivyLightColorScheme(),
        content = content,
    )
}

private fun ivyLightColorScheme(): ColorScheme = ColorScheme(
    primary = RevampLight.primary,
    onPrimary = IvyColors.White,
    primaryContainer = IvyColors.Purple.light,
    onPrimaryContainer = IvyColors.White,
    inversePrimary = IvyColors.Purple.dark,
    secondary = IvyColors.Green.primary,
    onSecondary = IvyColors.White,
    secondaryContainer = IvyColors.Green.light,
    onSecondaryContainer = IvyColors.White,
    tertiary = IvyColors.Green.primary,
    onTertiary = IvyColors.White,
    tertiaryContainer = IvyColors.Green.light,
    onTertiaryContainer = IvyColors.White,

    error = IvyColors.Red.primary,
    onError = IvyColors.White,
    errorContainer = IvyColors.Red.light,
    onErrorContainer = IvyColors.White,

    background = RevampLight.ground,
    onBackground = RevampLight.ink,
    surface = RevampLight.ground,
    onSurface = RevampLight.ink,
    surfaceVariant = IvyColors.ExtraLightGray,
    onSurfaceVariant = IvyColors.Black,
    surfaceTint = IvyColors.Black,
    inverseSurface = IvyColors.DarkGray,
    inverseOnSurface = IvyColors.White,

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.DarkGray,
    scrim = IvyColors.ExtraDarkGray.copy(alpha = 0.8f)
)

private fun ivyDarkColorScheme(isTrueBlack: Boolean): ColorScheme = ColorScheme(
    primary = RevampDark.primary,
    onPrimary = IvyColors.White,
    primaryContainer = IvyColors.Purple.light,
    onPrimaryContainer = IvyColors.White,
    inversePrimary = IvyColors.Purple.dark,
    secondary = IvyColors.Green.primary,
    onSecondary = IvyColors.White,
    secondaryContainer = IvyColors.Green.light,
    onSecondaryContainer = IvyColors.White,
    tertiary = IvyColors.Green.primary,
    onTertiary = IvyColors.White,
    tertiaryContainer = IvyColors.Green.light,
    onTertiaryContainer = IvyColors.White,

    error = IvyColors.Red.primary,
    onError = IvyColors.White,
    errorContainer = IvyColors.Red.light,
    onErrorContainer = IvyColors.White,

    background = if (isTrueBlack) RevampAmoled.ground else RevampDark.ground,
    onBackground = RevampDark.ink,
    surface = if (isTrueBlack) RevampAmoled.ground else RevampDark.ground,
    onSurface = RevampDark.ink,
    surfaceVariant = IvyColors.ExtraDarkGray,
    onSurfaceVariant = IvyColors.White,
    surfaceTint = IvyColors.White,
    inverseSurface = IvyColors.LightGray,
    inverseOnSurface = if (isTrueBlack) IvyColors.TrueBlack else IvyColors.Black,

    outline = IvyColors.Gray,
    outlineVariant = IvyColors.LightGray,
    scrim = IvyColors.ExtraLightGray.copy(alpha = 0.8f)
)
