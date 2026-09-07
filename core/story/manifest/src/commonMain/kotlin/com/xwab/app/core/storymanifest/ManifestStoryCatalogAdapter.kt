package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryCatalogPort
import com.xwab.app.core.story.port.StoryId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Serves the shipped manifest to screens as plain `Story` values, with sources left behind.
 *
 * It only queries a list, so it has no lifecycle — nothing to start, close or cancel. The
 * constructor takes the list so a test can query a small fixture instead of the real manifest.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class ManifestStoryCatalogAdapter internal constructor(
    stories: List<Story>,
) : StoryCatalogPort {
    @Inject
    internal constructor() : this(storyCatalog)
    private val allStories: Flow<List<Story>> = flowOf(stories)

    init {
        val duplicates = stories.groupBy { it.id }.filterValues { it.size > 1 }.keys
        // Two stories under one id make `observeStory` answer with whichever came first, which is
        // a bug that looks like a content mistake. A copied manifest row can produce it.
        require(duplicates.isEmpty()) {
            "Story ids must be unique: ${duplicates.joinToString { it.value }}"
        }
    }

    override fun observeStories(): Flow<List<Story>> = allStories

    override fun observeStory(storyId: StoryId): Flow<Story?> =
        allStories.map { values -> values.find { it.id == storyId } }
}
