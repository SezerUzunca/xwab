package com.xwab.app.feature.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.xwab.app.core.ui.components.MusicCard
import com.xwab.app.core.ui.components.PlayPauseButton
import com.xwab.app.core.ui.components.SleepRelaxBackground
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.feature.story.generated.resources.Res
import xwab.feature.story.generated.resources.stories_empty
import xwab.feature.story.generated.resources.stories_subtitle
import xwab.feature.story.generated.resources.stories_title
import xwab.feature.story.generated.resources.story_by
import xwab.feature.story.generated.resources.story_could_not_open
import xwab.feature.story.generated.resources.story_not_found
import xwab.feature.story.generated.resources.story_preparing
import xwab.feature.story.generated.resources.story_unavailable

@Composable
internal fun StoriesScreenRoute(viewModel: StoriesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StoriesScreen(
        state = state,
        onPlaybackClick = viewModel::togglePlayback,
    )
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
                    error = state.error.takeIf { state.failedStoryId == story.id },
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
    error: StoryError?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MusicCard(
            title = story.title,
            subtitle = stringResource(
                Res.string.story_by,
                story.author,
                formatStoryDuration(story.durationSeconds),
            ),
            onClick = onClick,
            trailingContent = {
                PlayPauseButton(isPlaying = isPlaying, onClick = onClick)
            },
        )
        if (isPreparing) {
            Text(
                text = stringResource(Res.string.story_preparing),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
                modifier = Modifier.padding(
                    start = SleepRelaxTheme.dimens.spacingMedium,
                    top = SleepRelaxTheme.dimens.spacingExtraSmall,
                ),
            )
        }
        error?.let {
            Text(
                text = stringResource(
                    when (it) {
                        StoryError.NotFound -> Res.string.story_not_found
                        StoryError.Unavailable -> Res.string.story_unavailable
                        StoryError.CouldNotOpen -> Res.string.story_could_not_open
                    },
                ),
                style = SleepRelaxTheme.typography.bodyMedium,
                color = SleepRelaxTheme.colors.error,
                modifier = Modifier.padding(
                    start = SleepRelaxTheme.dimens.spacingMedium,
                    top = SleepRelaxTheme.dimens.spacingExtraSmall,
                ),
            )
        }
    }
}

/**
 * The sound catalog has a formatter of its own, and this feature cannot see it: it declares
 * `core:story:catalog` and not `core:sound:catalog`. Four lines beats an edge to a capability this
 * screen has no other reason to read.
 */
private fun formatStoryDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    return "${safeSeconds / 60}:${(safeSeconds % 60).toString().padStart(2, '0')}"
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
