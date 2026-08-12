package com.xwab.app.feature.story.impl

import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.story.StoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * An in-memory story catalog, and the builder that makes a test's setup one line.
 *
 * Deliberately here rather than in `core:testing`. That module holds the fakes *every* feature
 * needs — the catalog, favorites and playback ports all three sound screens read — and adding a
 * story fake would put `core:story:catalog` on the test classpath of every feature, for the sake of
 * the one that reads it. It moves the day a second feature needs it, which is the same rule
 * `checkArchitecture` states for use cases.
 */
internal fun story(id: String, durationSeconds: Int = 180) = Story(
    id = StoryId(id),
    title = id,
    author = "Kate Chopin",
    description = "A test story.",
    narrator = "A narrator",
    durationSeconds = durationSeconds,
    artworkUrl = null,
)

internal class FakeStoryCatalog(
    private val stories: List<Story> = emptyList(),
) : StoryCatalogRepository {
    override fun observeStories(): Flow<List<Story>> = flowOf(stories)

    override fun observeStory(storyId: StoryId): Flow<Story?> =
        flowOf(stories.find { it.id == storyId })
}
