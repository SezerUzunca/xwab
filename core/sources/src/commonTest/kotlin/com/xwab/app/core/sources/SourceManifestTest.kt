package com.xwab.app.core.sources

import com.xwab.app.core.sources.port.ContentSource
import com.xwab.app.core.sources.port.SOUND_NAMESPACE
import com.xwab.app.core.sources.port.STORY_NAMESPACE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceManifestTest {
    @Test
    fun aSoundCacheNameCarriesTheItemIdAndVersion() {
        assertEquals(
            "heavy-rain-v2.mp3",
            soundSource("heavy-rain", "https://example.test/heavy-rain.mp3", version = 2)
                .source.cacheFileName,
        )
    }

    @Test
    fun unsafeIdsAndNonPositiveVersionsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            soundSource("White_Noise", "https://example.test/noise.mp3")
        }
        assertFailsWith<IllegalArgumentException> {
            soundSource("../etc/passwd", "https://example.test/noise.mp3")
        }
        assertFailsWith<IllegalArgumentException> {
            soundSource("", "https://example.test/noise.mp3")
        }
        assertFailsWith<IllegalArgumentException> {
            soundSource("heavy-rain", "https://example.test/noise.mp3", version = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            soundSource("heavy-rain", "https://example.test/noise.mp3", version = -1)
        }
    }

    @Test
    fun everyPhysicalAddressMustUseHttps() {
        assertFailsWith<IllegalArgumentException> {
            ContentSource("http://example.test/audio.mp3")
        }
        assertFailsWith<IllegalArgumentException> {
            soundSource("heavy-rain", "http://example.test/audio.mp3")
        }
    }

    /**
     * `ContentSource` validates a safe filename, not an audio one: `:core:sources` knows only that
     * its own manifests point at MP3s today, not that every source ever will.
     */
    @Test
    fun aCacheFilenameOfAnyExtensionIsAccepted() {
        val pdf = ContentSource(httpsUrl = "https://example.test/guide.pdf", cacheFileName = "guide-v1.pdf")
        val extensionless = ContentSource(httpsUrl = "https://example.test/blob", cacheFileName = "blob-v1")

        assertEquals("guide-v1.pdf", pdf.cacheFileName)
        assertEquals("blob-v1", extensionless.cacheFileName)
    }

    @Test
    fun blankRequestHeadersAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ContentSource("https://example.test/audio.mp3", headers = mapOf("" to "SleepSounds/1.0"))
        }
        assertFailsWith<IllegalArgumentException> {
            ContentSource("https://example.test/audio.mp3", headers = mapOf("User-Agent" to ""))
        }
    }

    /**
     * Wikimedia refuses a request that does not identify its client; that requirement is declared
     * on the source itself, not left for `:core:session` to know about the host it is fetching from.
     *
     * The contact URL is asserted rather than left to the string, because a product name on its own
     * is what the policy rejects — and dropping it would leave a header that still looks filled in.
     */
    @Test
    fun everySoundSourceCarriesTheHostsRequiredUserAgent() {
        soundSourceManifest.forEach { entry ->
            val userAgent = entry.source.headers["User-Agent"]
            assertEquals("SleepSounds/1.0 (https://github.com/SezerUzunca/xwab)", userAgent)
            assertTrue(userAgent.orEmpty().contains("https://"), "the policy asks for contact details")
        }
    }

    @Test
    fun shippedSourceRowsAreUniqueAndValid() {
        val allEntries = soundSourceManifest + storySourceManifest
        val urls = allEntries.map { it.source.httpsUrl }
        val cacheNames = soundSourceManifest.mapNotNull { it.source.cacheFileName }

        assertEquals(soundSourceManifest.size, soundSourceManifest.map { it.itemId }.toSet().size)
        assertEquals(storySourceManifest.size, storySourceManifest.map { it.itemId }.toSet().size)
        assertEquals(urls.size, urls.toSet().size, "duplicate source URLs")
        assertTrue(urls.all { it.endsWith(".mp3") })
        assertEquals(cacheNames.size, cacheNames.toSet().size, "duplicate cache filenames")
        assertEquals(soundSourceManifest.size, cacheNames.size, "every sound must be cached")
        assertTrue(storySourceManifest.all { it.source.cacheFileName == null })
        assertTrue(storySourceManifest.all { it.source.headers.isEmpty() })
    }

    @Test
    fun everyShippedSourceIsReachableThroughThePort() {
        val adapter = ManifestSourceAdapter()

        soundSourceManifest.forEach { entry ->
            assertEquals(entry.source, adapter.sourceFor(SOUND_NAMESPACE, entry.itemId))
        }
        storySourceManifest.forEach { entry ->
            assertEquals(entry.source, adapter.sourceFor(STORY_NAMESPACE, entry.itemId))
        }
        assertNull(adapter.sourceFor(SOUND_NAMESPACE, "no-such-track"))
    }
}
