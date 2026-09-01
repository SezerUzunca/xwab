package com.xwab.app.feature.story.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId
import com.xwab.app.core.playbacksession.PlaybackFailure
import com.xwab.app.core.ui.components.PlayableRow
import com.xwab.app.core.ui.format.formatDuration
import com.xwab.app.core.ui.components.LoadingContent
import com.xwab.app.core.ui.components.SleepRelaxBackground
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.core.ui.state.Loadable
import org.jetbrains.compose.resources.stringResource
import xwab.core.designsystem.generated.resources.Res as UiRes
import xwab.core.designsystem.generated.resources.preparing
import xwab.feature.story.`impl`.generated.resources.Res
import xwab.feature.story.`impl`.generated.resources.stories_empty
import xwab.feature.story.`impl`.generated.resources.stories_subtitle
import xwab.feature.story.`impl`.generated.resources.stories_title
import xwab.feature.story.`impl`.generated.resources.story_by
import xwab.feature.story.`impl`.generated.resources.story_could_not_open
import xwab.feature.story.`impl`.generated.resources.story_not_found
import xwab.feature.story.`impl`.generated.resources.story_unavailable

@Composable
internal fun StoriesScreenRoute(component: StoriesComponent) {
    val state by component.state.collectAsStateWithLifecycle()

    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> StoriesScreen(
            state = content.value,
            onPlaybackClick = component::togglePlayback,
        )
    }
}

/** A tab's root, so there is nothing above it to go back to and no back button on it. */
@Composable
internal fun StoriesScreen(
    state: StoriesState,
    onPlaybackClick: (storyId: StoryId) -> Unit,
) {
    SleepRelaxBackground {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                end = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                top = SleepRelaxTheme.dimens.paddingScreenVertical,
                bottom = SleepRelaxTheme.dimens.spacingHuge,
            ),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(Res.string.stories_title),
                        style = SleepRelaxTheme.typography.headlineSmall,
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
                item {
                    Text(
                        text = stringResource(Res.string.stories_empty),
                        style = SleepRelaxTheme.typography.bodyMedium,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                }
            }

            items(state.stories, key = { it.id.value }) { story ->
                StoryRow(
                    story = story,
                    isPlaying = state.requestedStoryId == story.id && state.playIntent,
                    isPreparing = state.requestedStoryId == story.id && state.isPreparing,
                    // Matched against the failure's own story, not the session's current one: a
                    // lookup that fails releases its claim, so the session has already moved on.
                    failure = state.playbackFailure?.takeIf { it.itemId.value == story.id.value },
                    onClick = { onPlaybackClick(story.id) },
                )
            }
        }
    }
}

@Composable
private fun StoryRow(
    story: Story,
    isPlaying: Boolean,
    isPreparing: Boolean,
    failure: PlaybackFailure?,
    onClick: () -> Unit,
) {
    // A story is played from its row and has no screen of its own, so opening it and starting it
    // are the same gesture.
    PlayableRow(
        title = story.title,
        subtitle = stringResource(
            Res.string.story_by,
            story.author,
            formatDuration(story.durationSeconds),
        ),
        isPlaying = isPlaying,
        onClick = onClick,
        onPlayPauseClick = onClick,
        statusMessage = stringResource(UiRes.string.preparing).takeIf { isPreparing },
        errorMessage = failure?.let { stringResource(it.messageResource()) },
    )
}

/**
 * A story the catalog has dropped and one that could not be reached read the same on screen
 * otherwise, and they are not the same advice: one is a dead end, the other is worth another tap.
 */
private fun PlaybackFailure.messageResource() = when (this) {
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
                        artworkUrl = null,
                    ),
                ),
            ),
            onPlaybackClick = {},
        )
    }
}
