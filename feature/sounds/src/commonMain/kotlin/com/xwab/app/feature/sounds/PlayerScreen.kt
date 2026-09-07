package com.xwab.app.feature.sounds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.FavoriteButton
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.SleepRelaxBackground
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.designsystem.state.Loadable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.duration_public_domain
import xwab.feature.sounds.generated.resources.Res
import xwab.feature.sounds.generated.resources.audio_could_not_open
import xwab.feature.sounds.generated.resources.audio_not_found
import xwab.feature.sounds.generated.resources.audio_unavailable
import xwab.feature.sounds.generated.resources.cancel_timer
import xwab.feature.sounds.generated.resources.loop_sound
import xwab.feature.sounds.generated.resources.offline_public_domain
import xwab.feature.sounds.generated.resources.sleep_timer
import xwab.feature.sounds.generated.resources.sleep_timer_off
import xwab.feature.sounds.generated.resources.sleep_timer_stops_in
import xwab.feature.sounds.generated.resources.timer_15_minutes
import xwab.feature.sounds.generated.resources.timer_30_minutes
import xwab.feature.sounds.generated.resources.timer_45_minutes
import xwab.feature.sounds.generated.resources.timer_60_minutes
import xwab.feature.sounds.generated.resources.volume
import xwab.feature.sounds.generated.resources.volume_percentage

private const val MINUTE_MS = 60_000L

/** The presets the timer row offers, in the order they are shown. */
private val SLEEP_TIMER_PRESETS: List<Pair<Long, StringResource>> = listOf(
    15L * MINUTE_MS to Res.string.timer_15_minutes,
    30L * MINUTE_MS to Res.string.timer_30_minutes,
    45L * MINUTE_MS to Res.string.timer_45_minutes,
    60L * MINUTE_MS to Res.string.timer_60_minutes,
)

@Composable
internal fun PlayerScreenRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> PlayerScreen(
            state = content.value,
            onBack = onBack,
            onFavoriteClick = viewModel::toggleFavorite,
            onPlaybackClick = viewModel::togglePlayback,
            onLoopingChange = viewModel::setLooping,
            onVolumeChange = viewModel::setVolume,
            onTimerStart = viewModel::startSleepTimer,
            onTimerCancel = viewModel::cancelSleepTimer,
        )
    }
}

@Composable
internal fun PlayerScreen(
    state: PlayerState,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
) {
    SleepRelaxBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isShortWindow = maxHeight < SleepRelaxTheme.dimens.playerShortWindowMaxHeight
            Column(
                modifier = Modifier
                    .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .then(
                        if (isShortWindow) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                    )
                    .padding(
                        horizontal = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                        vertical = SleepRelaxTheme.dimens.paddingScreenVertical,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BackButton(onClick = onBack)
                    FavoriteButton(
                        isFavorite = state.isFavorite,
                        onClick = onFavoriteClick,
                    )
                }

                if (isShortWindow) {
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                AlbumArt()

                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingHuge))
                Text(
                    text = state.music?.name.orEmpty(),
                    style = SleepRelaxTheme.typography.headlineSmall,
                    color = SleepRelaxTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                Text(
                    text = state.music?.let {
                        stringResource(UiRes.string.duration_public_domain, formatDuration(it.durationSeconds))
                    } ?: stringResource(Res.string.offline_public_domain),
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                PlayPauseButton(
                    isPlaying = state.playIntent,
                    onClick = onPlaybackClick,
                    large = true,
                )

                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                PlaybackControls(
                    state = state,
                    onLoopingChange = onLoopingChange,
                    onVolumeChange = onVolumeChange,
                    onTimerStart = onTimerStart,
                    onTimerCancel = onTimerCancel,
                )

                state.error?.let { error ->
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                    Text(
                        text = stringResource(
                            when (error) {
                                PlayerError.AudioNotFound -> Res.string.audio_not_found
                                PlayerError.AudioCouldNotOpen -> Res.string.audio_could_not_open
                                PlayerError.AudioUnavailable -> Res.string.audio_unavailable
                            },
                        ),
                        color = SleepRelaxTheme.colors.error,
                        style = SleepRelaxTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                if (isShortWindow) {
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Decoration only: the screen has no artwork to show, so the disc is drawn rather than loaded. */
@Composable
private fun AlbumArt() {
    Box(
        modifier = Modifier
            .size(SleepRelaxTheme.dimens.albumArtSize)
            .clip(SleepRelaxTheme.shapes.full)
            .background(
                Brush.radialGradient(
                    listOf(
                        SleepRelaxTheme.colors.primary.copy(alpha = 0.55f),
                        SleepRelaxTheme.colors.backgroundBottom,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SleepRelaxTheme.dimens.albumArtInnerSize)
                .clip(SleepRelaxTheme.shapes.full)
                .background(SleepRelaxTheme.colors.surface.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u266A",
                color = SleepRelaxTheme.colors.accent.copy(alpha = 0.7f),
                style = SleepRelaxTheme.typography.headlineLarge,
            )
        }
    }
}

/**
 * The glass panel under the play button. It only lays the three controls out; each one takes just
 * the part of the state it renders.
 */
@Composable
private fun PlaybackControls(
    state: PlayerState,
    onLoopingChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
) {
    // Nothing to act on until a track is loaded, and the same answer holds for every control.
    val controlsEnabled = state.music != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SleepRelaxTheme.shapes.medium)
            .background(SleepRelaxTheme.colors.glassWhite)
            .padding(SleepRelaxTheme.dimens.spacingLarge),
    ) {
        VolumeControl(
            volume = state.volume,
            enabled = controlsEnabled,
            onVolumeChange = onVolumeChange,
        )

        LoopingControl(
            isLooping = state.isLooping,
            enabled = controlsEnabled,
            onLoopingChange = onLoopingChange,
        )

        SleepTimerControl(
            remainingMs = state.sleepTimerRemainingMs,
            enabled = controlsEnabled,
            onTimerStart = onTimerStart,
            onTimerCancel = onTimerCancel,
        )
    }
}

/** The volume arrives from the state already inside its range, so the slider renders it as it is. */
@Composable
private fun VolumeControl(
    volume: Float,
    enabled: Boolean,
    onVolumeChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.volume),
            color = SleepRelaxTheme.colors.textPrimary,
            style = SleepRelaxTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(Res.string.volume_percentage, (volume * 100).toInt()),
            color = SleepRelaxTheme.colors.textSecondary,
            style = SleepRelaxTheme.typography.labelMedium,
        )
    }
    Slider(
        value = volume,
        onValueChange = onVolumeChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = SleepRelaxTheme.colors.accent,
            activeTrackColor = SleepRelaxTheme.colors.primary,
            inactiveTrackColor = SleepRelaxTheme.colors.glassWhiteOverlay,
        ),
    )
}

@Composable
private fun LoopingControl(
    isLooping: Boolean,
    enabled: Boolean,
    onLoopingChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.loop_sound),
            color = SleepRelaxTheme.colors.textPrimary,
            style = SleepRelaxTheme.typography.titleSmall,
        )
        Switch(
            checked = isLooping,
            onCheckedChange = onLoopingChange,
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
}

/**
 * Scoped to the column it sits in: the cancel button aligns itself to the panel's end, which only
 * that column can decide.
 */
@Composable
private fun ColumnScope.SleepTimerControl(
    remainingMs: Long?,
    enabled: Boolean,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
) {
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    Text(
        text = stringResource(Res.string.sleep_timer),
        color = SleepRelaxTheme.colors.textPrimary,
        style = SleepRelaxTheme.typography.titleSmall,
    )
    Text(
        text = remainingMs
            ?.let { stringResource(Res.string.sleep_timer_stops_in, formatSleepTimer(it)) }
            ?: stringResource(Res.string.sleep_timer_off),
        color = SleepRelaxTheme.colors.textSecondary,
        style = SleepRelaxTheme.typography.labelMedium,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingExtraSmall),
    ) {
        SLEEP_TIMER_PRESETS.forEach { (durationMs, label) ->
            TimerPresetButton(
                text = stringResource(label),
                durationMs = durationMs,
                enabled = enabled,
                onTimerStart = onTimerStart,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (remainingMs != null) {
        TextButton(
            onClick = onTimerCancel,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.textButtonColors(
                contentColor = SleepRelaxTheme.colors.accent,
            ),
        ) {
            Text(stringResource(Res.string.cancel_timer))
        }
    }
}

@Composable
private fun TimerPresetButton(
    text: String,
    durationMs: Long,
    enabled: Boolean,
    onTimerStart: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = { onTimerStart(durationMs) },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = SleepRelaxTheme.colors.accent,
        ),
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = SleepRelaxTheme.typography.labelMedium,
        )
    }
}

private fun formatSleepTimer(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview
@Composable
private fun PlayerScreenPreview() {
    SleepRelaxTheme {
        PlayerScreen(
            state = PlayerState(
                music = Music(
                    id = TrackId("calm-waves"),
                    name = "Ontario Waves",
                    categoryId = CategoryId("ocean"),
                    durationSeconds = 286,
                ),
                isFavorite = true,
                sleepTimerRemainingMs = 29L * MINUTE_MS + 42_000L,
            ),
            onBack = {},
            onFavoriteClick = {},
            onPlaybackClick = {},
            onLoopingChange = {},
            onVolumeChange = {},
            onTimerStart = {},
            onTimerCancel = {},
        )
    }
}
