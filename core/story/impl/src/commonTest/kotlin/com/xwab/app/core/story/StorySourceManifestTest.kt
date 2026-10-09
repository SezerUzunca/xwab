package com.xwab.app.core.story

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StorySourceManifestTest {
    @Test
    fun metadataAndPhysicalSourcesHaveExactlyTheSameIds() {
        assertEquals(
            storyManifest.map { it.id.value }.toSet(),
            storySourceManifest.map { it.itemId }.toSet(),
            "Each story owns both its metadata and its source; neither may be orphaned.",
        )
    }

    @Test
    fun shippedSourceRowsHaveUniqueIdsAndMp3Addresses() {
        val urls = storySourceManifest.map(StorySource::httpsUrl)

        assertEquals(storySourceManifest.size, storySourceManifest.map { it.itemId }.toSet().size)
        assertEquals(urls.size, urls.toSet().size, "duplicate source URLs")
        assertTrue(urls.all { it.endsWith(".mp3") })
    }

    @Test
    fun physicalSourcesRequireNonblankIdsAndHttpsAddresses() {
        listOf("", " ").forEach { id ->
            assertFailsWith<IllegalArgumentException> { StorySource(id, "https://example.test/story.mp3") }
        }
        listOf("", "http://example.test/story.mp3", "file:///story.mp3", "https://", "https://host/a b.mp3")
            .forEach { address ->
                assertFailsWith<IllegalArgumentException>(address) { StorySource("story", address) }
            }
    }
}
