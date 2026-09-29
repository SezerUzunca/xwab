package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.xwab.app.designsystem.format.formatRemaining
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.cancel_timer
import xwab.designsystem.generated.resources.sleep_timer
import xwab.designsystem.generated.resources.sleep_timer_off
import xwab.designsystem.generated.resources.sleep_timer_stops_in
import xwab.designsystem.generated.resources.sleep_timer_start_hint
import xwab.designsystem.generated.resources.sleep_timer_restart_hint
import xwab.designsystem.generated.resources.start_timer_duration
import xwab.designsystem.generated.resources.restart_timer_duration
import xwab.designsystem.generated.resources.set_sleep_timer
import xwab.designsystem.generated.resources.sleep_timer_running
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

/**
 * Stateless session control: callers observe the timer and own every command.
 *
 * The one thing a sleep app is opened for at night, so it is drawn to be found: a running timer is
 * the line in the accent colour with its cancel action beside it, and the presets are pills rather
 * than bare labels. The session reports only what is left, not which preset started it, so the
 * running countdown — not a highlighted preset — is what says which timer is on.
 */
@Composable
fun SleepTimerControl(
    remainingMs: Long?,
    enabled: Boolean,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingExtraSmall),
    ) {
        Text(
            text = stringResource(Res.string.sleep_timer),
            color = SleepRelaxTheme.colors.textPrimary,
            style = SleepRelaxTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        SleepTimerStatus(remainingMs, onTimerCancel)
        Text(
            text = stringResource(
                if (remainingMs == null) Res.string.sleep_timer_start_hint else Res.string.sleep_timer_restart_hint,
            ),
            color = SleepRelaxTheme.colors.textSecondary,
            style = SleepRelaxTheme.typography.labelMedium,
        )
        // Do not divide a narrow panel into four fixed slots: presets must remain readable with
        // large text too, and can flow onto a second line without horizontal scrolling.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            SLEEP_TIMER_PRESETS.forEach { (durationMs, label) ->
                val durationLabel = stringResource(label)
                val actionDescription = stringResource(
                    if (remainingMs == null) Res.string.start_timer_duration else Res.string.restart_timer_duration,
                    durationLabel,
                )
                SleepRelaxOutlinedButton(
                    onClick = { onTimerStart(durationMs) },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = actionDescription },
                ) {
                    Text(text = durationLabel, style = SleepRelaxTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SleepTimerStatus(remainingMs: Long?, onTimerCancel: () -> Unit) {
    if (remainingMs == null) {
        Text(
            text = stringResource(Res.string.sleep_timer_off),
            color = SleepRelaxTheme.colors.textSecondary,
            style = SleepRelaxTheme.typography.bodyMedium,
        )
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.sleep_timer_stops_in, formatRemaining(remainingMs)),
            modifier = Modifier.weight(1f),
            color = SleepRelaxTheme.colors.accent,
            style = SleepRelaxTheme.typography.titleMedium,
        )
        // Losing catalog content must never make an already running timer impossible to stop.
        SleepRelaxTextButton(onClick = onTimerCancel) {
            Text(stringResource(Res.string.cancel_timer))
        }
    }
}

/**
 * Where a screen about one item leads to the session's timer without drawing it.
 *
 * The timer stops whatever is playing, not the item on screen, so a content screen offers a way to
 * it rather than a copy of it: the one [SleepTimerControl] stays where it names what it will stop.
 * A running timer shows what is left, so the shortcut never offers to "set" one that is already on.
 */
@Composable
fun SleepTimerShortcut(onClick: () -> Unit, modifier: Modifier = Modifier, remainingMs: Long? = null) {
    SleepRelaxTextButton(onClick = onClick, modifier = modifier) {
        Text(
            remainingMs?.let { stringResource(Res.string.sleep_timer_running, formatRemaining(it)) }
                ?: stringResource(Res.string.set_sleep_timer),
        )
    }
}
