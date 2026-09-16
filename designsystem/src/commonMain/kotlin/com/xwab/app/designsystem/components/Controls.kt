package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/** Shared control styling; callers own values, labels and actions. */
@Composable
fun SleepRelaxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = SleepRelaxTheme.colors.accent,
            activeTrackColor = SleepRelaxTheme.colors.primary,
            inactiveTrackColor = SleepRelaxTheme.colors.glassWhiteOverlay,
        ),
    )
}

@Composable
fun SleepRelaxSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = SleepRelaxTheme.colors.onSurface,
            checkedTrackColor = SleepRelaxTheme.colors.primary,
            uncheckedThumbColor = SleepRelaxTheme.colors.textSecondary,
            uncheckedTrackColor = SleepRelaxTheme.colors.glassWhiteOverlay,
            uncheckedBorderColor = SleepRelaxTheme.colors.glassWhite,
        ),
    )
}

@Composable
fun SleepRelaxTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = SleepRelaxTheme.colors.accent),
        content = content,
    )
}
