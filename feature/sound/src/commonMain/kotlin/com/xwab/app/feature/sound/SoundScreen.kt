package com.xwab.app.feature.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.format.formatRemaining
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.FavoriteButton
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.SleepRelaxBackground
import com.xwab.app.designsystem.components.SleepRelaxSlider
import com.xwab.app.designsystem.components.SleepRelaxSwitch
import com.xwab.app.designsystem.components.SleepRelaxTextButton
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.designsystem.state.Loadable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.duration_public_domain
import xwab.designsystem.generated.resources.preparing
import xwab.designsystem.generated.resources.favorites_read_failed
import xwab.designsystem.generated.resources.favorite_write_failed
import xwab.feature.sound.generated.resources.Res
import xwab.feature.sound.generated.resources.cancel_timer
import xwab.feature.sound.generated.resources.loop_sound
import xwab.feature.sound.generated.resources.offline_public_domain
import xwab.feature.sound.generated.resources.sleep_timer
import xwab.feature.sound.generated.resources.sleep_timer_off
import xwab.feature.sound.generated.resources.sleep_timer_stops_in
import xwab.feature.sound.generated.resources.sound_could_not_open
import xwab.feature.sound.generated.resources.sound_not_found
import xwab.feature.sound.generated.resources.sound_unavailable
import xwab.feature.sound.generated.resources.timer_15_minutes
import xwab.feature.sound.generated.resources.timer_30_minutes
import xwab.feature.sound.generated.resources.timer_45_minutes
import xwab.feature.sound.generated.resources.timer_60_minutes
import xwab.feature.sound.generated.resources.volume
import xwab.feature.sound.generated.resources.volume_percentage

private const val MINUTE_MS = 60_000L
private val ALBUM_ART_SIZE = 220.dp
private val ALBUM_ART_INNER_SIZE = 100.dp

/** The presets the timer row offers, in the order they are shown. */
private val SLEEP_TIMER_PRESETS: List<Pair<Long, StringResource>> = listOf(
    15L * MINUTE_MS to Res.string.timer_15_minutes,
    30L * MINUTE_MS to Res.string.timer_30_minutes,
    45L * MINUTE_MS to Res.string.timer_45_minutes,
    60L * MINUTE_MS to Res.string.timer_60_minutes,
)

@Composable
internal fun SoundScreenRoute(
    onBack: () -> Unit,
    viewModel: SoundViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> SoundScreen(
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
internal fun SoundScreen(
    state: SoundState,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
) {
    SleepRelaxBackground {
        // Content can outgrow a tall window too when text scales or status lines appear.
        Column(
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
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
                    enabled = state.canFavorite,
                )
            }

            if (!state.favoritesAvailable || state.favoriteWriteFailed) {
                Text(
                    text = stringResource(if (!state.favoritesAvailable) UiRes.string.favorites_read_failed else UiRes.string.favorite_write_failed),
                    color = SleepRelaxTheme.colors.error,
                    style = SleepRelaxTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            AlbumArt()

            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingHuge))
            Text(
                text = state.track?.name.orEmpty(),
                style = SleepRelaxTheme.typography.headlineSmall,
                color = SleepRelaxTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
            Text(
                text = state.track?.let {
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
                enabled = state.canPlay,
            )

            // The same line every list draws under a row that is wanted but not audible yet.
            // This screen worked the state out and then drew nothing with it, so a source that
            // took a moment to resolve looked like a button that had missed the tap.
            if (state.isPreparing) {
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                Text(
                    text = stringResource(UiRes.string.preparing),
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

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
                            SoundError.SoundNotFound -> Res.string.sound_not_found
                            SoundError.SoundCouldNotOpen -> Res.string.sound_could_not_open
                            SoundError.SoundUnavailable -> Res.string.sound_unavailable
                        },
                    ),
                    color = SleepRelaxTheme.colors.error,
                    style = SleepRelaxTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
        }
    }
}

/** Decoration only: the screen has no artwork to show, so the disc is drawn rather than loaded. */
@Composable
private fun AlbumArt() {
    Box(
        modifier = Modifier
            .size(ALBUM_ART_SIZE)
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
                .size(ALBUM_ART_INNER_SIZE)
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
    state: SoundState,
    onLoopingChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(SleepRelaxTheme.dimens.spacingLarge),
    ) {
        VolumeControl(
            volume = state.volume,
            enabled = state.canConfigure,
            onVolumeChange = onVolumeChange,
        )

        LoopingControl(
            isLooping = state.isLooping,
            enabled = state.canConfigure,
            onLoopingChange = onLoopingChange,
        )

        SleepTimerControl(
            remainingMs = state.sleepTimerRemainingMs,
            enabled = state.canConfigure,
            onTimerStart = onTimerStart,
            onTimerCancel = onTimerCancel,
        )
    }
}

/** The session states and keeps the volume range, so the slider renders what it is handed. */
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
    SleepRelaxSlider(
        value = volume,
        onValueChange = onVolumeChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
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
        SleepRelaxSwitch(
            checked = isLooping,
            onCheckedChange = onLoopingChange,
            enabled = enabled,
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
            ?.let { stringResource(Res.string.sleep_timer_stops_in, formatRemaining(it)) }
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
        SleepRelaxTextButton(
            onClick = onTimerCancel,
            modifier = Modifier.align(Alignment.End),
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
    SleepRelaxTextButton(
        onClick = { onTimerStart(durationMs) },
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = SleepRelaxTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
private fun SoundScreenPreview() {
    SleepRelaxTheme {
        SoundScreen(
            state = SoundState(
                track = Track(
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
