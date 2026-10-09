package com.xwab.app.feature.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.result.ResultEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.designsystem.components.PlayableRow
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.preparing
import xwab.feature.story.generated.resources.Res
import xwab.feature.story.generated.resources.stories_empty
import xwab.feature.story.generated.resources.stories_subtitle
import xwab.feature.story.generated.resources.stories_title
import xwab.feature.story.generated.resources.story_by
import xwab.feature.story.generated.resources.story_could_not_open
import xwab.feature.story.generated.resources.story_not_found
import xwab.feature.story.generated.resources.story_unavailable

@Composable
internal fun StoriesScreenRoute(
    viewModel: StoriesViewModel,
    onStoryClick: (StoryId) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    ResultEffect<StoriesRoute> { scope.launch { listState.animateScrollToItem(0) } }

    when (val content = state) {
        StoriesUiState.Loading -> LoadingContent()
        is StoriesUiState.Ready -> StoriesScreen(
            state = content.value,
            onPlaybackClick = viewModel::togglePlayback,
            onStoryClick = onStoryClick,
            listState = listState,
        )
    }
}

/** A tab's root, so there is nothing above it to go back to and no back button on it. */
@Composable
internal fun StoriesScreen(
    state: StoriesState,
    onPlaybackClick: (storyId: StoryId) -> Unit,
    onStoryClick: (StoryId) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    ScreenContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            item(key = "header") {
                Column {
                    Text(
                        text = stringResource(Res.string.stories_title),
                        modifier = Modifier.semantics { heading() },
                        // The same size as the other two tab titles; a tab root is not a sub-page.
                        style = SleepRelaxTheme.typography.headlineLarge,
                        color = SleepRelaxTheme.colors.textPrimary,
                    )
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                    Text(
                        text = stringResource(Res.string.stories_subtitle),
                        style = SleepRelaxTheme.typography.bodyLarge,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                }
            }

            if (state.stories.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(Res.string.stories_empty),
                        style = SleepRelaxTheme.typography.bodyMedium,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                }
            }

            items(state.stories, key = { "story:${it.id.value}" }) { story ->
                // Every question about this row is the state's to answer; this only draws what
                // comes back.
                StoryRow(
                    story = story,
                    state = state,
                    onClick = dropUnlessResumed { onStoryClick(story.id) },
                    onPlayPauseClick = { onPlaybackClick(story.id) },
                )
            }
        }
    }
}

@Composable
private fun StoryRow(
    story: Story,
    state: StoriesState,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
) {
    PlayableRow(
        title = story.title,
        subtitle = stringResource(
            Res.string.story_by,
            story.author,
            formatDuration(story.durationSeconds),
        ),
        playRequested = state.isRowPlaying(story.id),
        onClick = onClick,
        onPlayPauseClick = onPlayPauseClick,
        statusMessage = stringResource(UiRes.string.preparing).takeIf { state.isRowPreparing(story.id) },
        errorMessage = state.rowFailure(story.id)?.let { stringResource(it.storyMessageResource()) },
    )
}

/**
 * A story the catalog has dropped and one that could not be reached read the same on screen
 * otherwise, and they are not the same advice: one is a dead end, the other is worth another tap.
 */
internal fun PlaybackFailure.storyMessageResource() = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.story_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.story_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.story_could_not_open
}

@Preview
@Composable
private fun StoriesScreenPreview() {
    SleepRelaxTheme {
        StoriesScreen(
            state = StoriesState(
                stories = listOf(
                    Story(
                        id = StoryId("night-came-slowly"),
                        title = "The Night Came Slowly",
                        author = "Kate Chopin",
                        description = "A quiet meditation on dusk.",
                        narrator = "Alan Davis Drake",
                        durationSeconds = 174,
                    ),
                ),
            ),
            onPlaybackClick = {},
            onStoryClick = {},
        )
    }
}
