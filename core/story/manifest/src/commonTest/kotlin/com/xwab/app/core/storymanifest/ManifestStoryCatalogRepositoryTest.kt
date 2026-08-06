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
    private val lantern = story("forest-lantern")
    private val harbour = story("harbour-night")
    private val repository = ManifestStoryCatalogRepository(stories = listOf(lantern, harbour))

    @Test
    fun theWholeManifestIsServedAsGiven() = runBlocking {
        assertEquals(listOf(lantern, harbour), repository.observeStories().first())
    }

    @Test
    fun oneStoryIsServedById() = runBlocking {
        assertEquals(harbour, repository.observeStory(StoryId("harbour-night")).first())
    }

    /**
     * A screen opened on a story the feed has since dropped — a restored back stack, a stale deep
     * link — has to see an empty result rather than a flow that never emits.
     */
    @Test
    fun anUnknownIdEmitsNothingRatherThanNeverEmitting() = runBlocking {
        assertNull(repository.observeStory(StoryId("no-such-story")).first())
    }

    @Test
    fun twoStoriesUnderOneIdAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ManifestStoryCatalogRepository(
                stories = listOf(lantern, story("forest-lantern", title = "A Second Lantern")),
            )
        }
    }

    /** The shipped manifest is what the app actually serves, so it goes through the same checks. */
    @Test
    fun theShippedManifestIsServed() = runBlocking {
        val repository = ManifestStoryCatalogRepository()

        assertEquals(storyManifest, repository.observeStories().first())
        assertEquals(
            storyManifest.first(),
            repository.observeStory(storyManifest.first().id).first(),
        )
    }

    private fun story(id: String, title: String = id) = Story(
        id = StoryId(id),
        title = title,
        description = "A placeholder story.",
        narrator = "Mira",
        durationSeconds = 600,
        artworkUrl = null,
    )
}
