package com.xwab.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xwab.app.designsystem.format.remainingWholeMinutes
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.restart_timer
import xwab.designsystem.generated.resources.sleep_timer
import xwab.designsystem.generated.resources.sleep_timer_off
import xwab.designsystem.generated.resources.sleep_timer_stops_in
import xwab.designsystem.generated.resources.timer_all_night
import xwab.designsystem.generated.resources.timer_duration_minutes

private const val MINUTE_MS = 60_000L
private val TIMER_PRESET_MINUTES = listOf(15, 30, 60, 90)

/**
 * Five choices: all night, or one of the presets. The selected choice is what is set, so
 * there is no separate Start, Restart or Cancel.
 *
 * - A duration sets the timer from now; the selected one again restarts it. The caller decides
 *   whether that also starts playback.
 * - All night clears the timer: a sound plays until someone stops it, a story until it ends.
 *
 * Callers observe the timer and own its commands; a timer tick never moves the selection.
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
        TimerChoices(remainingMs = remainingMs, onTimerStart = onTimerStart, onTimerCancel = onTimerCancel)
    }
}

@Composable
private fun TimerChoices(remainingMs: Long?, onTimerStart: (Long) -> Unit, onTimerCancel: () -> Unit) {
    val isRunning = remainingMs != null
    // The preset tapped here. The session only reports what is left, and inferring the preset from
    // that on every tick would move the selection down to a shorter preset as time runs out.
    var chosenMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    // A timer already running when this screen opened: infer its preset once, then hold it.
    val inferredMinutes = remember(isRunning) { remainingMs?.let(::presetMinutesFor) }
    LaunchedEffect(isRunning) { if (!isRunning) chosenMinutes = null }
    val selectedMinutes = if (isRunning) chosenMinutes ?: inferredMinutes else null

    val restartLabel = stringResource(Res.string.restart_timer)

    // Off on its own line, the durations together below it. Each chip keeps its own width so that
    // large text wraps to another line instead of being cut off.
    Column(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
    ) {
        TimerChip(
            label = stringResource(Res.string.timer_all_night),
            selected = !isRunning,
            onClick = { if (isRunning) onTimerCancel() },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            TIMER_PRESET_MINUTES.forEach { minutes ->
                val selected = selectedMinutes == minutes
                TimerChip(
                    label = stringResource(Res.string.timer_duration_minutes, minutes),
                    selected = selected,
                    // The selected duration still acts: it starts the countdown again.
                    onClickLabel = restartLabel.takeIf { selected },
                    onClick = {
                        chosenMinutes = minutes
                        onTimerStart(minutes * MINUTE_MS)
                    },
                )
            }
        }
    }
}

/**
 * A single-choice pill. Not Material's FilterChip: that one tells a screen reader it is a checkbox,
 * and these are one choice out of five.
 */
@Composable
private fun TimerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
) {
    val shape = SleepRelaxTheme.shapes.full
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(if (selected) SleepRelaxTheme.colors.accent else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else SleepRelaxTheme.colors.glassWhiteOverlay, shape)
            .semantics { this.selected = selected }
            .clickable(onClickLabel = onClickLabel, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = SleepRelaxTheme.dimens.spacingMedium, vertical = SleepRelaxTheme.dimens.spacingSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) SleepRelaxTheme.colors.backgroundBottom else SleepRelaxTheme.colors.textPrimary,
            style = SleepRelaxTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

/**
 * The preset a running timer most likely came from: the shortest one that still covers what is
 * left, since a timer only ever counts down from its preset.
 */
internal fun presetMinutesFor(remainingMs: Long): Int {
    val minutesLeft = remainingWholeMinutes(remainingMs)
    return TIMER_PRESET_MINUTES.firstOrNull { it >= minutesLeft } ?: TIMER_PRESET_MINUTES.last()
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
