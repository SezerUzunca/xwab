package com.xwab.app.core.story

import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves metadata from one immutable manifest. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class ManifestStoryCatalogAdapter internal constructor(stories: List<Story>) : StoryPort {
    @Inject
    internal constructor() : this(storyManifest)

    init {
        val duplicates = stories.groupBy(Story::id).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Story ids must be unique: ${duplicates.joinToString { it.value }}"
        }
    }

    private val allStories = flowOf(stories)

    override fun observeStories(): Flow<List<Story>> = allStories
    override fun observeStory(storyId: StoryId): Flow<Story?> =
        allStories.map { values -> values.find { it.id == storyId } }
}
