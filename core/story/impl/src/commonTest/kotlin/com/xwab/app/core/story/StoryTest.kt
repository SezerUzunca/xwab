package com.xwab.app.core.story.port

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

    /** A narrator is optional; blank strings are not the way to say so. */
    @Test
    fun anAbsentNarratorIsFine() {
        val story = story(narrator = null)

        assertNull(story.narrator)
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


    private fun story(
        title: String = "The Night Came Slowly",
        author: String = "Kate Chopin",
        description: String = "A quiet meditation on dusk.",
        narrator: String? = "Alan Davis Drake",
        durationSeconds: Int = 174,
    ) = Story(
        id = StoryId("night-came-slowly"),
        title = title,
        author = author,
        description = description,
        narrator = narrator,
        durationSeconds = durationSeconds,
    )
}
