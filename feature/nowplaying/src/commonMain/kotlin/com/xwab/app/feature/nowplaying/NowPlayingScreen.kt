package com.xwab.app.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.SleepRelaxSlider
import com.xwab.app.designsystem.components.SleepTimerControl
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.feature.nowplaying.generated.resources.Res
import xwab.feature.nowplaying.generated.resources.device_volume_note
import xwab.feature.nowplaying.generated.resources.now_playing
import xwab.feature.nowplaying.generated.resources.player_empty
import xwab.feature.nowplaying.generated.resources.player_volume
import xwab.feature.nowplaying.generated.resources.player_volume_value
import xwab.feature.nowplaying.generated.resources.repeat_playback
import xwab.feature.nowplaying.generated.resources.view_details

@Composable
internal fun NowPlayingScreenRoute(
    onBack: () -> Unit,
    onOpenDetails: (PlaybackItemId) -> Unit,
    viewModel: NowPlayingViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NowPlayingScreen(
        state = state,
        onBack = dropUnlessResumed { onBack() },
        onPlayPause = viewModel::togglePlayback,
        onVolumeChange = viewModel::setVolume,
        onLoopingChange = viewModel::setLooping,
        onTimerStart = viewModel::startSleepTimer,
        onTimerCancel = viewModel::cancelSleepTimer,
        onOpenDetails = dropUnlessResumed { state.itemId?.let(onOpenDetails) },
    )
}

/** Stateless session controls, hosted and scoped by a Navigation 3 entry. */
@Composable
@Suppress("LongParameterList")
internal fun NowPlayingScreen(
    state: NowPlayingState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Text(
                    text = stringResource(Res.string.now_playing),
                    modifier = Modifier.semantics { heading() },
                    style = SleepRelaxTheme.typography.headlineSmall,
                    color = SleepRelaxTheme.colors.textPrimary,
                )
            }
            if (state.isIdle) {
                Text(stringResource(Res.string.player_empty), color = SleepRelaxTheme.colors.textSecondary)
            } else {
                NowPlayingItem(state, onPlayPause, onOpenDetails)
            }
            // Directly under the transport, in a card of its own: the timer is what a sleep app is
            // opened for at night. Usable with nothing requested — the timer is the session's, so it
            // can be set before a sound is chosen, and one outliving playback can still be cancelled.
            SleepTimerControl(
                remainingMs = state.sleepTimerRemainingMs,
                enabled = true,
                onTimerStart = onTimerStart,
                onTimerCancel = onTimerCancel,
                modifier = Modifier.glassCard().padding(SleepRelaxTheme.dimens.spacingMedium),
            )
            if (!state.isIdle) {
                PlayerVolumeControls(state.volume, onVolumeChange)
                RepeatPlaybackControl(state.isLooping, onLoopingChange)
            }
        }
    }
}

/** Centred like the sound and story details, so the item and its action read as one block. */
@Composable
private fun NowPlayingItem(state: NowPlayingState, onPlayPause: () -> Unit, onOpenDetails: () -> Unit) {
    val title = state.title ?: stringResource(Res.string.now_playing)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = SleepRelaxTheme.typography.headlineSmall,
            color = SleepRelaxTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        PlaybackStatus(state)
        PlayPauseButton(
            playRequested = state.playIntent,
            onClick = onPlayPause,
            large = true,
            contentTitle = title,
        )
        TextButton(onClick = onOpenDetails) { Text(stringResource(Res.string.view_details)) }
    }
}

@Composable
private fun PlayerVolumeControls(volume: Float, onVolumeChange: (Float) -> Unit) {
    val volumeLabel = stringResource(Res.string.player_volume)
    Text(
        text = stringResource(Res.string.player_volume_value, (volume * PERCENT_SCALE).toInt()),
        style = SleepRelaxTheme.typography.titleSmall,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    SleepRelaxSlider(
        value = volume,
        onValueChange = onVolumeChange,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = volumeLabel },
    )
    Text(
        text = stringResource(Res.string.device_volume_note),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}

@Composable
private fun RepeatPlaybackControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.repeat_playback),
            modifier = Modifier.weight(1f),
            style = SleepRelaxTheme.typography.titleSmall,
            color = SleepRelaxTheme.colors.textPrimary,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

private const val PERCENT_SCALE = 100
