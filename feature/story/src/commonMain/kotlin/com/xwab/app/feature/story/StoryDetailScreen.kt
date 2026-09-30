package com.xwab.app.feature.story

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.xwab.app.core.story.port.Story
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.PlaybackSettingsCard
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.preparing
import xwab.feature.story.generated.resources.Res
import xwab.feature.story.generated.resources.story_by
import xwab.feature.story.generated.resources.story_narrator
import xwab.feature.story.generated.resources.story_not_found
import xwab.feature.story.generated.resources.internet_required

@Composable
internal fun StoryDetailScreenRoute(
    viewModel: StoryDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    when (val content = state) {
        StoryDetailUiState.Loading -> LoadingContent()
        is StoryDetailUiState.Ready -> StoryDetailScreen(
            state = content.value,
            onBack = onBack,
            onPlaybackClick = viewModel::togglePlayback,
            onLoopingChange = viewModel::setLooping,
            onTimerStart = viewModel::startSleepTimer,
            onTimerCancel = viewModel::cancelSleepTimer,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
        )
    }
}

/**
 * Opening a story is informational; only the explicit transport button changes playback.
 * Laid out like the sound detail: the story's name and its action centred, the session's timer and
 * repeat in a card, then the story's text left-aligned.
 */
@Composable
@Suppress("LongParameterList") // Screen events plus the timer the card reports.
internal fun StoryDetailScreen(
    state: StoryDetailState,
    onBack: () -> Unit,
    onPlaybackClick: () -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    sleepTimerRemainingMs: Long? = null,
) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) { BackButton(onClick = dropUnlessResumed(block = onBack)) }
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            state.story?.let { StoryMetadata(it) }
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            PlayPauseButton(
                playRequested = state.playIntent,
                onClick = onPlaybackClick,
                enabled = state.canPlay,
                large = true,
                contentTitle = state.story?.title,
            )
            StoryStatus(state)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            PlaybackSettingsCard(
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                isLooping = state.isLooping,
                onTimerStart = onTimerStart,
                onTimerCancel = onTimerCancel,
                onLoopingChange = onLoopingChange,
            )
            state.story?.let { story ->
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                Text(
                    story.description,
                    modifier = Modifier.fillMaxWidth(),
                    style = SleepRelaxTheme.typography.bodyLarge,
                    color = SleepRelaxTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun StoryStatus(state: StoryDetailState) {
    if (state.isPreparing) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
        Text(
            stringResource(UiRes.string.preparing),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    val error = if (state.story == null) {
        Res.string.story_not_found
    } else state.failure?.storyMessageResource()
    error?.let {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(it),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StoryMetadata(story: Story) {
    Text(
        story.title,
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.headlineSmall,
        color = SleepRelaxTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    Text(
        stringResource(Res.string.story_by, story.author, formatDuration(story.durationSeconds)),
        style = SleepRelaxTheme.typography.bodyMedium,
        color = SleepRelaxTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
    story.narrator?.let { narrator ->
        Text(
            stringResource(Res.string.story_narrator, narrator),
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
    Text(
        stringResource(Res.string.internet_required),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}
