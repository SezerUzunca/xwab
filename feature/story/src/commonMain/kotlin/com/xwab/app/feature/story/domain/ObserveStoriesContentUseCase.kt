package com.xwab.app.feature.story.domain

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class StoriesContent(
    val stories: List<Story>,
    val playback: PlaybackSummary,
)

/**
 * Joins the two ports this screen reads into the one thing it shows.
 *
 * Two rather than the three the sound screens join: there is no favorites port for stories, so
 * nothing else is combined here. Feature-owned for the same reason as the others — only the ports
 * are shared, never the question a screen asks of them.
 */
internal class ObserveStoriesContentUseCase(
    private val storyPort: StoryPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(): Flow<StoriesContent> = combine(
        storyPort.observeStories(),
        playbackPort.playback,
    ) { stories, playback ->
        StoriesContent(stories = stories, playback = playback)
    }
}
