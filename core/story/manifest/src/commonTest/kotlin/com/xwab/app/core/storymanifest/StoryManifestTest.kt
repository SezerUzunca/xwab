package com.xwab.app.core.storymanifest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The manifest is hand-written today and parsed from a feed later. These are the properties that
 * have to hold either way, checked here so a copied row fails a build rather than a screen.
 */
class StoryManifestTest {
    @Test
    fun theManifestIsNotEmpty() {
        assertTrue(storyManifest.isNotEmpty(), "the story manifest ships no stories")
    }

    @Test
    fun everyStoryHasItsOwnId() {
        val ids = storyManifest.map { it.id }

        assertEquals(ids.size, ids.toSet().size, "two stories share an id: $ids")
    }

    /**
     * `Story` refuses a blank title and a duration that is not positive, so every entry above has
     * already been through those checks by the time this list exists. This test is what makes that
     * true of the *shipped* list rather than of a fixture.
     */
    @Test
    fun everyStoryIsListable() {
        storyManifest.forEach { story ->
            assertTrue(story.title.isNotBlank(), "${story.id} has no title")
            assertTrue(story.description.isNotBlank(), "${story.id} has no description")
            assertTrue(story.durationSeconds > 0, "${story.id} has no duration")
        }
    }
}
