package com.xwab.app.feature.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.FavoriteButton
import com.xwab.app.feature.sound.domain.SoundFavoriteReadStatus
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.components.SleepRelaxSlider
import com.xwab.app.designsystem.components.SleepRelaxSwitch
import com.xwab.app.designsystem.components.SleepTimerControl
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.duration_public_domain
import xwab.designsystem.generated.resources.preparing
import xwab.designsystem.generated.resources.favorites_read_failed
import xwab.designsystem.generated.resources.favorite_write_failed
import xwab.feature.sound.generated.resources.Res
import xwab.feature.sound.generated.resources.loop_sound
import xwab.feature.sound.generated.resources.sound_could_not_open
import xwab.feature.sound.generated.resources.sound_not_found
import xwab.feature.sound.generated.resources.sound_unavailable
import xwab.feature.sound.generated.resources.volume
import xwab.feature.sound.generated.resources.volume_is_separate
import xwab.feature.sound.generated.resources.volume_percentage

private const val MINUTE_MS = 60_000L
private val ALBUM_ART_SIZE = 220.dp
private val ALBUM_ART_INNER_SIZE = 100.dp

@Composable
internal fun SoundScreenRoute(
    onBack: () -> Unit,
    viewModel: SoundViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val content = state) {
        SoundUiState.Loading -> LoadingContent()
        is SoundUiState.Ready -> SoundScreen(
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
    ScreenContainer {
        // Content can outgrow a tall window too when text scales or status lines appear.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
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
                    isLoading = state.favoriteReadStatus == SoundFavoriteReadStatus.Pending,
                )
            }

            if (state.favoriteReadStatus == SoundFavoriteReadStatus.Unavailable || state.favoriteWriteFailed) {
                Text(
                    text = stringResource(if (state.favoriteReadStatus == SoundFavoriteReadStatus.Unavailable) UiRes.string.favorites_read_failed else UiRes.string.favorite_write_failed),
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
            // Only when there is a sound to describe. This used to fall back to
            // "Offline • Public Domain", which said two things about a track the catalog does not
            // hold: that it is available without a network, and that its licence is known. Neither
            // is knowable about something that is not there — and the error below already says
            // what actually happened.
            state.track?.let { track ->
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                Text(
                    text = stringResource(
                        UiRes.string.duration_public_domain,
                        formatDuration(track.durationSeconds),
                    ),
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

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

        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
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
    val label = stringResource(Res.string.volume)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
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
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        enabled = enabled,
    )
    // The device's own media volume multiplies with this one and is what the phone's volume keys
    // move, so a silenced device leaves this slider reading 100% over silence. Said here rather
    // than fixed by binding the two, because turning a sleep sound down at night should not turn
    // the whole phone down.
    Text(
        text = stringResource(Res.string.volume_is_separate),
        color = SleepRelaxTheme.colors.textSecondary,
        style = SleepRelaxTheme.typography.labelMedium,
    )
}

@Composable
private fun LoopingControl(
    isLooping: Boolean,
    enabled: Boolean,
    onLoopingChange: (Boolean) -> Unit,
) {
    val label = stringResource(Res.string.loop_sound)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = SleepRelaxTheme.colors.textPrimary,
            style = SleepRelaxTheme.typography.titleSmall,
        )
        SleepRelaxSwitch(
            checked = isLooping,
            onCheckedChange = onLoopingChange,
            modifier = Modifier.semantics { contentDescription = label },
            enabled = enabled,
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
                favoriteReadStatus = SoundFavoriteReadStatus.Available,
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
