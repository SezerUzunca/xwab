package com.xwab.app.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun SleepRelaxTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSleepRelaxColors provides darkColors,
        LocalSleepRelaxDimens provides SleepRelaxDimens(),
        LocalSleepRelaxTypography provides SleepRelaxTypography(),
        LocalSleepRelaxShapes provides SleepRelaxShapes(),
        content = content,
    )
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
