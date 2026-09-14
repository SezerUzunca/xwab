package com.xwab.app.core.sources

import com.xwab.app.core.sources.port.ContentSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ManifestSourceAdapterTest {
    private val cached = ContentSource(
        httpsUrl = "https://example.test/heavy-rain.mp3",
        cacheFileName = "heavy-rain-v1.mp3",
    )
    private val streamed = ContentSource("https://example.test/night.mp3")
    private val adapter = ManifestSourceAdapter(
        mapOf(
            "sound" to listOf(ManifestSource("heavy-rain", cached)),
            "story" to listOf(ManifestSource("night", streamed)),
        ),
    )

    @Test
    fun aPublishedItemIsResolvedInsideItsNamespace() {
        assertEquals(cached, adapter.sourceFor("sound", "heavy-rain"))
        assertEquals(streamed, adapter.sourceFor("story", "night"))
    }

    @Test
    fun unknownNamespacesAndItemsHaveNoSource() {
        assertNull(adapter.sourceFor("sound", "night"))
        assertNull(adapter.sourceFor("unknown", "heavy-rain"))
    }

    @Test
    fun cacheInventoryContainsOnlyCachedSourcesInThatNamespace() {
        assertEquals(setOf("heavy-rain-v1.mp3"), adapter.cacheFileNames("sound"))
        assertEquals(emptySet(), adapter.cacheFileNames("story"))
        assertEquals(emptySet(), adapter.cacheFileNames("unknown"))
    }

    @Test
    fun duplicateIdsInsideOneNamespaceAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ManifestSourceAdapter(
                mapOf(
                    "sound" to listOf(
                        ManifestSource("heavy-rain", cached),
                        ManifestSource("heavy-rain", cached),
                    ),
                ),
            )
        }
    }
}
