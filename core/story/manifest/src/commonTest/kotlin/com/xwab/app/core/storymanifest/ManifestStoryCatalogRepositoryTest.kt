package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ManifestStoryCatalogRepositoryTest {
    private val night = story("night-came-slowly")
    private val idleFellow = story("an-idle-fellow")
    private val repository = ManifestStoryCatalogRepository(stories = listOf(night, idleFellow))

    @Test
    fun theWholeManifestIsServedAsGiven() = runBlocking {
        assertEquals(listOf(night, idleFellow), repository.observeStories().first())
    }

    @Test
    fun oneStoryIsServedById() = runBlocking {
        assertEquals(idleFellow, repository.observeStory(StoryId("an-idle-fellow")).first())
    }

    /**
     * A restored back stack or stale deep link has to see an empty result rather than a flow that
     * never emits.
     */
    @Test
    fun anUnknownIdEmitsNothingRatherThanNeverEmitting() = runBlocking {
        assertNull(repository.observeStory(StoryId("no-such-story")).first())
    }

    @Test
    fun twoStoriesUnderOneIdAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ManifestStoryCatalogRepository(
                stories = listOf(night, story("night-came-slowly", title = "Another Version")),
            )
        }
    }

    /** The shipped manifest is what the app actually serves, so it goes through the same checks. */
    @Test
    fun theShippedManifestIsServed() = runBlocking {
        val repository = ManifestStoryCatalogRepository()

        assertEquals(storyCatalog, repository.observeStories().first())
        assertEquals(
            storyCatalog.first(),
            repository.observeStory(storyCatalog.first().id).first(),
        )
    }

    private fun story(id: String, title: String = id) = Story(
        id = StoryId(id),
        title = title,
        author = "Kate Chopin",
        description = "A literary short story.",
        narrator = "Alan Davis Drake",
        durationSeconds = 600,
        artworkUrl = null,
    )
}
