package com.xwab.app.feature.story.domain

import dev.zacsweers.metro.Inject

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.story.port.StoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class StoryContent(val story: Story?, val playback: PlaybackSummary)

/** The detail observes one catalog entry, independently of the list's lifecycle. */
@Inject
internal class ObserveStoryContentUseCase(
    private val storyPort: StoryPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(storyId: StoryId): Flow<StoryContent> = combine(
        storyPort.observeStory(storyId),
        playbackPort.playback,
    ) { story, playback -> StoryContent(story, playback) }
}
