package com.xwab.app.feature.sound

import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.FavoriteButton
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.PlaybackSettingsCard
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.sound.domain.SoundFavoriteReadStatus
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.duration_public_domain
import xwab.designsystem.generated.resources.loop_duration
import xwab.designsystem.generated.resources.favorite_write_failed
import xwab.designsystem.generated.resources.favorites_read_failed
import xwab.designsystem.generated.resources.preparing
import xwab.feature.sound.generated.resources.Res
import xwab.feature.sound.generated.resources.sound_could_not_open
import xwab.feature.sound.generated.resources.sound_not_found
import xwab.feature.sound.generated.resources.sound_unavailable
import xwab.feature.sound.generated.resources.available_offline
import xwab.feature.sound.generated.resources.internet_required

private const val PREVIEW_DURATION_SECONDS = 286

@Composable
internal fun SoundDetailScreenRoute(
    onBack: () -> Unit,
    viewModel: SoundViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    when (val content = state) {
        SoundUiState.Loading -> LoadingContent()
        is SoundUiState.Ready -> SoundDetailScreen(
            state = content.value,
            onBack = onBack,
            onFavoriteClick = viewModel::toggleFavorite,
            onPlaybackClick = viewModel::togglePlayback,
            onLoopingChange = viewModel::setLooping,
            onTimerStart = viewModel::startSleepTimer,
            onTimerCancel = viewModel::cancelSleepTimer,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
        )
    }
}

/** The sound and its actions, then the session's timer and repeat in one card. */
@Composable
@Suppress("LongParameterList") // Screen events plus the timer the card reports.
internal fun SoundDetailScreen(
    state: SoundState,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    sleepTimerRemainingMs: Long? = null,
) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = dropUnlessResumed(block = onBack))
                // The host hides Back while the parent pane is beside this one. The spacer, not the
                // arrangement, keeps the heart at the end whether or not Back is drawn.
                Spacer(Modifier.weight(1f))
                FavoriteButton(
                    isFavorite = state.isFavorite,
                    onClick = onFavoriteClick,
                    enabled = state.canFavorite,
                    isLoading = state.favoriteReadStatus == SoundFavoriteReadStatus.Pending,
                    contentTitle = state.track?.name,
                )
            }
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            SoundPlaceholderArtwork()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            SoundMetadata(state)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            PlayPauseButton(
                playRequested = state.playIntent,
                onClick = onPlaybackClick,
                large = true,
                enabled = state.canPlay,
                contentTitle = state.track?.name,
            )
            SoundStatus(state)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            PlaybackSettingsCard(
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                isLooping = state.isLooping,
                onTimerStart = onTimerStart,
                onTimerCancel = onTimerCancel,
                onLoopingChange = onLoopingChange,
            )
        }
    }
}

@Composable
private fun SoundMetadata(state: SoundState) {
    val track = state.track ?: return
    Text(
        text = track.name,
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.headlineSmall,
        color = SleepRelaxTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
    Text(
        text = stringResource(
            UiRes.string.duration_public_domain,
            stringResource(UiRes.string.loop_duration, formatDuration(track.durationSeconds)),
        ),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
    val availability = if (state.availableOffline) Res.string.available_offline else Res.string.internet_required
    Text(
        text = stringResource(availability),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}

@Composable
private fun SoundStatus(state: SoundState) {
    if (state.isPreparing) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
        Text(
            text = stringResource(UiRes.string.preparing),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    val playbackError = when (state.error) {
        SoundError.SoundNotFound -> Res.string.sound_not_found
        SoundError.SoundCouldNotOpen -> Res.string.sound_could_not_open
        SoundError.SoundUnavailable -> Res.string.sound_unavailable
        null -> null
    }
    val favoriteError = when {
        state.favoriteReadStatus == SoundFavoriteReadStatus.Unavailable -> UiRes.string.favorites_read_failed
        state.favoriteWriteFailed -> UiRes.string.favorite_write_failed
        else -> null
    }
    listOfNotNull(playbackError, favoriteError).forEach { error ->
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            text = stringResource(error),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = SleepRelaxTheme.colors.error,
            style = SleepRelaxTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Compact decoration leaves the content and its actions together on small screens. */
@Composable
private fun SoundPlaceholderArtwork() {
    Box(
        modifier = Modifier.size(112.dp).clearAndSetSemantics {}.clip(SleepRelaxTheme.shapes.full)
            .background(Brush.radialGradient(listOf(
                SleepRelaxTheme.colors.primary.copy(alpha = 0.55f),
                SleepRelaxTheme.colors.backgroundBottom,
            ))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u266A",
            color = SleepRelaxTheme.colors.accent.copy(alpha = 0.7f),
            style = SleepRelaxTheme.typography.headlineLarge,
        )
    }
}

@Preview
@Composable
private fun SoundDetailScreenPreview() {
    SleepRelaxTheme {
        SoundDetailScreen(
            state = SoundState(
                track = Track(
                    TrackId("calm-waves"),
                    "Ontario Waves",
                    CategoryId("ocean"),
                    durationSeconds = PREVIEW_DURATION_SECONDS,
                ),
                isFavorite = true,
                favoriteReadStatus = SoundFavoriteReadStatus.Available,
            ),
            onBack = {},
            onFavoriteClick = {},
            onPlaybackClick = {},
            onLoopingChange = {},
            onTimerStart = {},
            onTimerCancel = {},
        )
    }
}
