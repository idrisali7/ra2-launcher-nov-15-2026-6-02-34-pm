package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = SovietCrimson,
    onPrimary = TextPrimary,
    primaryContainer = SovietCrimsonContainer,
    onPrimaryContainer = OnSovietCrimsonContainer,
    secondary = AlliedCyan,
    onSecondary = TacticalVoid,
    secondaryContainer = AlliedCyanContainer,
    onSecondaryContainer = OnAlliedCyanContainer,
    tertiary = TacticalAmber,
    onTertiary = TacticalVoid,
    tertiaryContainer = TacticalAmberContainer,
    onTertiaryContainer = TextPrimary,
    background = TacticalVoid,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = TacticalVoid,
    surfaceContainerLow = TacticalSurface,
    surfaceContainer = TacticalSurfaceElevated,
    surfaceContainerHigh = TacticalSurfaceBright,
    outline = TacticalBorder,
    outlineVariant = TacticalBorder.copy(alpha = 0.55f),
    error = SovietCrimsonBright,
    onError = TacticalVoid,
    errorContainer = SovietCrimsonContainer,
    onErrorContainer = OnSovietCrimsonContainer
)

private val TacticalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun RA2LauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = AppTypography,
        shapes = TacticalShapes,
        content = content
    )
}
