package com.xwab.app.feature.story.impl.domain

import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackSummary
import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryCatalogRepository
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
    private val storyCatalog: StoryCatalogRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(): Flow<StoriesContent> = combine(
        storyCatalog.observeStories(),
        playbackCoordinator.playback,
    ) { stories, playback ->
        StoriesContent(stories = stories, playback = playback)
    }
}
