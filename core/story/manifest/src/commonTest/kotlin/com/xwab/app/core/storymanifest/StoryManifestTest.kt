package com.xwab.app.core.storymanifest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * These are the properties of the shipped hand-written manifest. A copied or incomplete row fails
 * the build rather than a listener's tap.
 */
class StoryManifestTest {
    @Test
    fun theManifestIsNotEmpty() {
        assertTrue(storyManifest.isNotEmpty(), "the story manifest ships no stories")
    }

    @Test
    fun everyStoryHasItsOwnId() {
        val ids = storyManifest.map { it.story.id }

        assertEquals(ids.size, ids.toSet().size, "two stories share an id: $ids")
    }

    /**
     * `Story` refuses a blank title and a duration that is not positive, so every entry above has
     * already been through those checks by the time this list exists. This test is what makes that
     * true of the *shipped* list rather than of a fixture.
     */
    @Test
    fun everyStoryIsListable() {
        storyManifest.map(StoryEntry::story).forEach { story ->
            assertTrue(story.title.isNotBlank(), "${story.id} has no title")
            assertTrue(story.author.isNotBlank(), "${story.id} has no author")
            assertTrue(story.description.isNotBlank(), "${story.id} has no description")
            assertTrue(!story.narrator.isNullOrBlank(), "${story.id} has no narrator")
            assertTrue(story.durationSeconds > 0, "${story.id} has no duration")
        }
    }

    @Test
    fun everyStoryHasItsOwnSource() {
        val sources = storyManifest.map(StoryEntry::httpsUrl)

        assertEquals(sources.size, sources.toSet().size, "two stories share an audio source")
        assertTrue(sources.all { it.startsWith("https://") }, "a story source is not HTTPS")
    }
}
