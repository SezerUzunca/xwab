package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.xwab.app.designsystem.format.remainingWholeMinutes
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.cancel_timer
import xwab.designsystem.generated.resources.sleep_timer
import xwab.designsystem.generated.resources.sleep_timer_off
import xwab.designsystem.generated.resources.sleep_timer_stops_in
import xwab.designsystem.generated.resources.start_timer_duration
import xwab.designsystem.generated.resources.restart_timer_duration
import xwab.designsystem.generated.resources.timer_minutes
import xwab.designsystem.generated.resources.timer_duration_minutes
import xwab.designsystem.generated.resources.timer_15_minutes
import xwab.designsystem.generated.resources.timer_30_minutes
import xwab.designsystem.generated.resources.timer_60_minutes
import xwab.designsystem.generated.resources.timer_90_minutes
import xwab.designsystem.generated.resources.start_timer
import xwab.designsystem.generated.resources.restart_timer

private const val MINUTE_MS = 60_000L
private const val DEFAULT_PRESET_INDEX = 1
private val TIMER_PRESETS: List<Pair<Int, StringResource>> = listOf(
    15 to Res.string.timer_15_minutes,
    30 to Res.string.timer_30_minutes,
    60 to Res.string.timer_60_minutes,
    90 to Res.string.timer_90_minutes,
)
private val TIMER_MINUTE_VALUES = TIMER_PRESETS.map { it.first }

/**
 * Callers observe the timer and own its commands. The wheel holds only a draft duration:
 * scrolling never changes the running timer, and Start/Restart explicitly commits the selection.
 * A timer tick does not move the wheel.
 *
 * Laid out to fit under an item's own controls on a phone, with the now-playing bar showing too:
 * the status sits on the title's line, and the wheel and its actions sit side by side.
 */
@Composable
fun SleepTimerControl(
    remainingMs: Long?,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.sleep_timer),
                color = SleepRelaxTheme.colors.textPrimary,
                style = SleepRelaxTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            SleepTimerStatus(remainingMs)
        }
        TimerDurationSelection(remainingMs = remainingMs, onTimerStart = onTimerStart, onTimerCancel = onTimerCancel)
    }
}

@Composable
private fun TimerDurationSelection(remainingMs: Long?, onTimerStart: (Long) -> Unit, onTimerCancel: () -> Unit) {
    val isRunning = remainingMs != null
    // Opened on a running timer, the wheel starts at the duration that timer most likely came from,
    // so Restart repeats it rather than silently switching to the default.
    val wheelState = rememberLazyListState(initialFirstVisibleItemIndex = initialPresetIndex(remainingMs))
    val selectedIndex by remember(wheelState) {
        derivedStateOf { wheelState.centeredWheelIndex().coerceIn(TIMER_PRESETS.indices) }
    }
    val selectedPreset = TIMER_PRESETS[selectedIndex]
    val durationMs = selectedPreset.first * MINUTE_MS
    val durationLabels = TIMER_PRESETS.map { stringResource(it.second) }
    val actionDescription = stringResource(
        if (isRunning) Res.string.restart_timer_duration else Res.string.start_timer_duration,
        durationLabels[selectedIndex],
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DurationWheel(
            state = wheelState,
            values = TIMER_MINUTE_VALUES,
            valueLabel = { durationLabels[TIMER_MINUTE_VALUES.indexOf(it)] },
            label = stringResource(Res.string.timer_minutes),
            modifier = Modifier.weight(1f),
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingExtraSmall),
        ) {
            Button(
                onClick = { onTimerStart(durationMs) },
                enabled = !wheelState.isScrollInProgress,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = actionDescription },
                shape = SleepRelaxTheme.shapes.full,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleepRelaxTheme.colors.accent,
                    contentColor = SleepRelaxTheme.colors.backgroundBottom,
                ),
            ) {
                Text(stringResource(if (isRunning) Res.string.restart_timer else Res.string.start_timer))
            }
            // Losing catalog content must never make an already running timer impossible to stop.
            if (isRunning) {
                SleepRelaxTextButton(onClick = onTimerCancel) {
                    Text(stringResource(Res.string.cancel_timer))
                }
            }
        }
    }
}

/**
 * The preset a running timer most likely came from: the shortest one that still covers what is
 * left, since a timer only ever counts down from its preset. The default when nothing runs.
 */
internal fun initialPresetIndex(remainingMs: Long?): Int {
    if (remainingMs == null) return DEFAULT_PRESET_INDEX
    val minutesLeft = remainingWholeMinutes(remainingMs)
    return TIMER_MINUTE_VALUES.indexOfFirst { it >= minutesLeft }.takeIf { it >= 0 } ?: TIMER_MINUTE_VALUES.lastIndex
}

/** "Off", or what is left in whole minutes, as the now-playing bar shows it too. */
@Composable
private fun SleepTimerStatus(remainingMs: Long?) {
    if (remainingMs == null) {
        Text(
            text = stringResource(Res.string.sleep_timer_off),
            color = SleepRelaxTheme.colors.textSecondary,
            style = SleepRelaxTheme.typography.bodyMedium,
        )
        return
    }
    Text(
        text = stringResource(
            Res.string.sleep_timer_stops_in,
            stringResource(Res.string.timer_duration_minutes, remainingWholeMinutes(remainingMs)),
        ),
        color = SleepRelaxTheme.colors.accent,
        style = SleepRelaxTheme.typography.titleSmall,
    )
}

@Preview
@Composable
private fun SleepTimerControlPreview() {
    SleepRelaxTheme {
        Column(verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium)) {
            SleepTimerControl(remainingMs = null, onTimerStart = {}, onTimerCancel = {})
            SleepTimerControl(remainingMs = 840_000L, onTimerStart = {}, onTimerCancel = {})
        }
    }
}
