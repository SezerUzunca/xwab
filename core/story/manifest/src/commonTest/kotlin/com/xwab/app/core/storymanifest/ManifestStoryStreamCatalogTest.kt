package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** The physical-source port read only by the playback session. */
class ManifestStoryStreamCatalogTest {
    @Test
    fun aStoryIsAnsweredWithItsStreamSource() {
        val catalog = ManifestStoryStreamCatalog(
            listOf(entry("night-came-slowly", "https://example.test/night.mp3")),
        )

        assertEquals(
            StoryStreamSource("https://example.test/night.mp3"),
            catalog.sourceFor(StoryId("night-came-slowly")),
        )
    }

    @Test
    fun aStoryTheManifestDoesNotHoldHasNoSource() {
        assertNull(ManifestStoryStreamCatalog().sourceFor(StoryId("no-such-story")))
    }

    /**
     * Story audio goes straight to the platform player, so the manifest and the value crossing
     * that boundary both reject cleartext addresses.
     */
    @Test
    fun aSourceThatIsNotHttpsIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            entry("night-came-slowly", "http://example.test/night.mp3")
        }
        assertFailsWith<IllegalArgumentException> {
            StoryStreamSource("http://example.test/night.mp3")
        }
    }

    @Test
    fun everyShippedStoryIsReachableThroughThePort() {
        val catalog = ManifestStoryStreamCatalog()

        storyManifest.forEach { entry ->
            assertEquals(
                StoryStreamSource(entry.httpsUrl),
                catalog.sourceFor(entry.story.id),
                "${entry.story.id} has no playable source",
            )
        }
    }

    private fun entry(id: String, httpsUrl: String) = StoryEntry(
        story = Story(
            id = StoryId(id),
            title = "The Night Came Slowly",
            author = "Kate Chopin",
            description = "A literary short story.",
            narrator = "Alan Davis Drake",
            durationSeconds = 174,
            artworkUrl = null,
        ),
        httpsUrl = httpsUrl,
    )
}
