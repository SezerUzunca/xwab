package com.xwab.app.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme

private val appTypography = SleepRelaxTypography()
private val appShapes = SleepRelaxShapes()
private val appDimens = SleepRelaxDimens()
private val materialColors = darkColorScheme(
    primary = darkColors.primary,
    onPrimary = darkColors.backgroundBottom,
    primaryContainer = darkColors.surface,
    onPrimaryContainer = darkColors.accent,
    secondary = darkColors.accent,
    onSecondary = darkColors.backgroundBottom,
    secondaryContainer = darkColors.surface,
    onSecondaryContainer = darkColors.accent,
    tertiary = darkColors.accent,
    onTertiary = darkColors.backgroundBottom,
    tertiaryContainer = darkColors.surface,
    onTertiaryContainer = darkColors.accent,
    background = darkColors.backgroundBottom,
    onBackground = darkColors.textPrimary,
    surface = darkColors.surface,
    surfaceDim = darkColors.backgroundBottom,
    surfaceBright = darkColors.surface,
    surfaceContainerLowest = darkColors.backgroundBottom,
    surfaceContainerLow = darkColors.surface,
    surfaceContainer = darkColors.surface,
    surfaceContainerHigh = darkColors.surface,
    surfaceContainerHighest = darkColors.surface,
    onSurface = darkColors.onSurface,
    surfaceVariant = darkColors.surface,
    onSurfaceVariant = darkColors.textSecondary,
    inverseSurface = darkColors.onSurface,
    inverseOnSurface = darkColors.surface,
    inversePrimary = darkColors.surface,
    surfaceTint = darkColors.primary,
    outline = darkColors.textSecondary,
    outlineVariant = darkColors.glassWhiteOverlay,
    error = darkColors.error,
    onError = darkColors.backgroundBottom,
    errorContainer = darkColors.surface,
    onErrorContainer = darkColors.error,
)
private val materialTypography = Typography(
    displayLarge = appTypography.headlineLarge,
    displayMedium = appTypography.headlineMedium,
    displaySmall = appTypography.headlineSmall,
    headlineLarge = appTypography.headlineLarge,
    headlineMedium = appTypography.headlineMedium,
    headlineSmall = appTypography.headlineSmall,
    titleLarge = appTypography.titleLarge,
    titleMedium = appTypography.titleMedium,
    titleSmall = appTypography.titleSmall,
    bodyLarge = appTypography.bodyLarge,
    bodyMedium = appTypography.bodyMedium,
    bodySmall = appTypography.bodySmall,
    labelLarge = appTypography.titleSmall,
    labelMedium = appTypography.labelMedium,
    labelSmall = appTypography.labelMedium,
)
private val materialShapes = Shapes(
    extraSmall = appShapes.small,
    small = appShapes.small,
    medium = appShapes.medium,
    large = appShapes.large,
    extraLarge = appShapes.large,
)

@Composable
fun SleepRelaxTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSleepRelaxColors provides darkColors,
        LocalSleepRelaxDimens provides appDimens,
        LocalSleepRelaxTypography provides appTypography,
        LocalSleepRelaxShapes provides appShapes,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = materialTypography,
            shapes = materialShapes,
            content = content,
        )
    }
}

object SleepRelaxTheme {
    val colors: SleepRelaxColors
        @Composable
        get() = LocalSleepRelaxColors.current

    val dimens: SleepRelaxDimens
        @Composable
        get() = LocalSleepRelaxDimens.current

    val typography: SleepRelaxTypography
        @Composable
        get() = LocalSleepRelaxTypography.current

    val shapes: SleepRelaxShapes
        @Composable
        get() = LocalSleepRelaxShapes.current
}
