package com.xwab.app.core.story

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The model is built from a hand-written list today and from a parsed feed later, so what it
 * refuses here is what would otherwise reach a screen as a blank card or an undrawable progress
 * bar.
 */
class StoryTest {
    @Test
    fun aStoryKeepsTheMetadataItWasGiven() {
        val story = story()

        assertEquals(StoryId("forest-lantern"), story.id)
        assertEquals("The Forest Lantern", story.title)
        assertEquals("Mira", story.narrator)
        assertEquals(900, story.durationSeconds)
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
            story(artworkUrl = "http://example.test/forest-lantern.jpg")
        }
    }

    private fun story(
        title: String = "The Forest Lantern",
        narrator: String? = "Mira",
        durationSeconds: Int = 900,
        artworkUrl: String? = "https://example.test/forest-lantern.jpg",
    ) = Story(
        id = StoryId("forest-lantern"),
        title = title,
        description = "A slow walk through a winter forest.",
        narrator = narrator,
        durationSeconds = durationSeconds,
        artworkUrl = artworkUrl,
    )
}
