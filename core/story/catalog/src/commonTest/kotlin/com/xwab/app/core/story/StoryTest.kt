package com.xwab.app.core.story

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * What the model refuses here would otherwise reach a screen as incomplete literary metadata or
 * an undrawable progress bar.
 */
class StoryTest {
    @Test
    fun aStoryKeepsTheMetadataItWasGiven() {
        val story = story()

        assertEquals(StoryId("night-came-slowly"), story.id)
        assertEquals("The Night Came Slowly", story.title)
        assertEquals("Kate Chopin", story.author)
        assertEquals("Alan Davis Drake", story.narrator)
        assertEquals(174, story.durationSeconds)
    }

    /** A narrator and artwork are genuinely optional; blank strings are not the way to say so. */
    @Test
    fun anAbsentNarratorAndArtworkAreFine() {
        val story = story(narrator = null, artworkUrl = null)

        assertNull(story.narrator)
        assertNull(story.artworkUrl)
    }

    @Test
    fun aStoryWithoutATitleIsRejected() {
        assertFailsWith<IllegalArgumentException> { story(title = "") }
        assertFailsWith<IllegalArgumentException> { story(title = "  ") }
    }

    @Test
    fun aStoryWithoutAnAuthorOrDescriptionIsRejected() {
        assertFailsWith<IllegalArgumentException> { story(author = " ") }
        assertFailsWith<IllegalArgumentException> { story(description = "") }
    }

    @Test
    fun aDurationThatIsNotPositiveIsRejected() {
        assertFailsWith<IllegalArgumentException> { story(durationSeconds = 0) }
        assertFailsWith<IllegalArgumentException> { story(durationSeconds = -1) }
    }

    @Test
    fun aBlankNarratorIsRejected() {
        assertFailsWith<IllegalArgumentException> { story(narrator = " ") }
    }

    @Test
    fun artworkThatIsNotHttpsIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            story(artworkUrl = "http://example.test/night-came-slowly.jpg")
        }
    }

    private fun story(
        title: String = "The Night Came Slowly",
        author: String = "Kate Chopin",
        description: String = "A quiet meditation on dusk.",
        narrator: String? = "Alan Davis Drake",
        durationSeconds: Int = 174,
        artworkUrl: String? = "https://example.test/night-came-slowly.jpg",
    ) = Story(
        id = StoryId("night-came-slowly"),
        title = title,
        author = author,
        description = description,
        narrator = narrator,
        durationSeconds = durationSeconds,
        artworkUrl = artworkUrl,
    )
}
