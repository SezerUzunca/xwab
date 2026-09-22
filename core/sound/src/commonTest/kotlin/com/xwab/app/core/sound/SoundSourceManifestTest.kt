package com.xwab.app.core.sound

import com.xwab.app.core.sound.port.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoundSourceManifestTest {
    @Test
    fun metadataAndPhysicalSourcesHaveExactlyTheSameIds() {
        assertEquals(
            catalogManifest.map { it.id }.toSet(),
            soundSourceManifest.map { it.trackId }.toSet(),
            "Each sound owns both its metadata and its source; neither may be orphaned.",
        )
    }

    @Test
    fun everyPublishedTrackHasAValidatedRequestWithTheCompleteCacheInventory() {
        val sources = SoundSources()
        val requests = soundSourceManifest.associate { it.trackId to it.request }
        val expectedCacheInventory = requests.values.map { it.key.fileName }.toSet()

        catalogManifest.forEach { track ->
            assertEquals(
                requests.getValue(track.id).copy(retainedFileNames = expectedCacheInventory),
                sources.requestFor(track.id),
            )
        }
    }

    @Test
    fun aSoundCacheNameCarriesTheItemIdAndVersion() {
        assertEquals(
            "heavy-rain-v2.mp3",
            soundSource("heavy-rain", "https://example.test/heavy-rain.mp3", version = 2).request.key.fileName,
        )
    }

    @Test
    fun unsafeIdsAndNonPositiveVersionsAreRejected() {
        listOf("White_Noise", "../etc/passwd", "").forEach { itemId ->
            assertFailsWith<IllegalArgumentException> {
                soundSource(itemId, "https://example.test/noise.mp3")
            }
        }
        listOf(0, -1).forEach { version ->
            assertFailsWith<IllegalArgumentException> {
                soundSource("heavy-rain", "https://example.test/noise.mp3", version)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            soundSource("heavy-rain", "http://example.test/audio.mp3")
        }
    }

    @Test
    fun everySoundSourceCarriesTheHostsRequiredUserAgent() {
        soundSourceManifest.forEach { entry ->
            val userAgent = entry.request.headers["User-Agent"]
            assertEquals("SleepSounds/1.0 (https://github.com/SezerUzunca/xwab)", userAgent)
            assertTrue(userAgent.orEmpty().contains("https://"), "the policy asks for contact details")
        }
    }

    @Test
    fun shippedSourceRowsHaveUniqueIdsUrlsAndCacheFiles() {
        val urls = soundSourceManifest.map { it.request.httpsUrl }
        val cacheNames = soundSourceManifest.map { it.request.key.fileName }

        assertEquals(soundSourceManifest.size, soundSourceManifest.map { it.trackId }.toSet().size)
        assertEquals(urls.size, urls.toSet().size, "duplicate source URLs")
        assertTrue(urls.all { it.endsWith(".mp3") })
        assertEquals(cacheNames.size, cacheNames.toSet().size, "duplicate cache filenames")
        assertEquals(soundSourceManifest.size, cacheNames.size, "every sound must be cached")
    }

    @Test
    fun duplicateSourceIdsAreRejectedBeforeTheyCanShadowEachOther() {
        val first = soundSource("rain", "https://example.test/rain.mp3")
        val second = soundSource("rain", "https://example.test/new-rain.mp3", version = 2)

        assertFailsWith<IllegalArgumentException> { SoundSources(listOf(first, second)) }
    }

    @Test
    fun twoTracksCannotClaimTheSameCacheFile() {
        val first = soundSource("rain", "https://example.test/rain.mp3")
        val second = first.copy(trackId = TrackId("other-rain"))

        assertFailsWith<IllegalArgumentException> { SoundSources(listOf(first, second)) }
    }
}
