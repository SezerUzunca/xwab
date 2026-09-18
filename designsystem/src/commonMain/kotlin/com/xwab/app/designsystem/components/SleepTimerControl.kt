package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xwab.app.designsystem.format.formatRemaining
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.cancel_timer
import xwab.designsystem.generated.resources.sleep_timer
import xwab.designsystem.generated.resources.sleep_timer_off
import xwab.designsystem.generated.resources.sleep_timer_stops_in
import xwab.designsystem.generated.resources.timer_15_minutes
import xwab.designsystem.generated.resources.timer_30_minutes
import xwab.designsystem.generated.resources.timer_45_minutes
import xwab.designsystem.generated.resources.timer_60_minutes

private const val MINUTE_MS = 60_000L
private val SLEEP_TIMER_PRESETS: List<Pair<Long, StringResource>> = listOf(
    15L * MINUTE_MS to Res.string.timer_15_minutes,
    30L * MINUTE_MS to Res.string.timer_30_minutes,
    45L * MINUTE_MS to Res.string.timer_45_minutes,
    60L * MINUTE_MS to Res.string.timer_60_minutes,
)

/** Stateless session control: callers observe the timer and own every command. */
@Composable
fun SleepTimerControl(
    remainingMs: Long?,
    enabled: Boolean,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.sleep_timer),
            color = SleepRelaxTheme.colors.textPrimary,
            style = SleepRelaxTheme.typography.titleSmall,
        )
        Text(
            text = remainingMs
                ?.let { stringResource(Res.string.sleep_timer_stops_in, formatRemaining(it)) }
                ?: stringResource(Res.string.sleep_timer_off),
            color = SleepRelaxTheme.colors.textSecondary,
            style = SleepRelaxTheme.typography.labelMedium,
        )
        // Do not divide a narrow panel into four fixed slots: presets must remain readable with
        // large text too, and can flow onto a second line without horizontal scrolling.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingExtraSmall),
        ) {
            SLEEP_TIMER_PRESETS.forEach { (durationMs, label) ->
                SleepRelaxTextButton(
                    onClick = { onTimerStart(durationMs) },
                    enabled = enabled,
                ) {
                    Text(
                        text = stringResource(label),
                        style = SleepRelaxTheme.typography.labelMedium,
                    )
                }
            }
        }
        if (remainingMs != null) {
            // Losing catalog content must never make an already running timer impossible to stop.
            SleepRelaxTextButton(
                onClick = onTimerCancel,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.cancel_timer))
            }
        }
    }
}
