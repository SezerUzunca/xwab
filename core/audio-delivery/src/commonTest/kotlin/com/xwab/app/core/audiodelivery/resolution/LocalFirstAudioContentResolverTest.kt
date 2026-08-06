package com.xwab.app.core.audiodelivery.resolution

import com.xwab.app.core.audiodelivery.FakeAudioSourceCatalog
import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.catalogmanifest.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

/**
 * Source selection only. Since the resolver hands fetching to [AudioPrefetcher], these tests assert
 * *what was asked for* rather than waiting for a download, so none of them depend on timing.
 */
class LocalFirstAudioContentResolverTest {
    @Test
    fun anUncachedTrackStreamsAndIsQueuedForLater() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val resolver = LocalFirstAudioContentResolver(FakeAudioFileStore(), prefetcher, CATALOG)

        val resolution = assertIs<AudioSourceResolution.Resolved>(resolver.resolve(TRACK))

        assertEquals(REMOTE_URL, resolution.uri)
        assertEquals(listOf(CACHE_FILE_NAME to REMOTE_URL), prefetcher.requests)
    }

    @Test
    fun anAlreadyCachedTrackResolvesToTheLocalFileAndIsNotQueuedAgain() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val fileStore = FakeAudioFileStore(
            cached = mapOf(CACHE_FILE_NAME to "file:///audio/$CACHE_FILE_NAME"),
        )
        val resolver = LocalFirstAudioContentResolver(fileStore, prefetcher, CATALOG)

        val resolution = assertIs<AudioSourceResolution.Resolved>(resolver.resolve(TRACK))

        assertEquals("file:///audio/$CACHE_FILE_NAME", resolution.uri)
        assertTrue(prefetcher.requests.isEmpty())
    }

    /**
     * Named rather than null, because the session tells the two outcomes apart on screen: a track
     * the catalog dropped is a dead end, while an unreachable source is worth another tap.
     */
    @Test
    fun unknownContentIsNotFoundAndStartsNoPrefetch() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val resolver = LocalFirstAudioContentResolver(FakeAudioFileStore(), prefetcher, CATALOG)

        assertEquals(AudioSourceResolution.NotFound, resolver.resolve(TrackId("no-such-track")))

        assertTrue(prefetcher.requests.isEmpty())
    }

    /**
     * A failure below this line used to escape into the caller's coroutine, where it was dropped —
     * a tap that produced no sound and no reason. It comes back as an outcome the session can show.
     */
    @Test
    fun aLookupThatFailsOutrightIsUnavailableRatherThanAnEscapingError() = runBlocking {
        val resolver = LocalFirstAudioContentResolver(
            object : AudioFileStore {
                override suspend fun find(cacheFileName: String): String? =
                    error("the cache is unreadable")

                override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) = Unit
            },
            RecordingPrefetcher(),
            CATALOG,
        )

        val resolution = assertIs<AudioSourceResolution.Unavailable>(resolver.resolve(TRACK))

        assertEquals("the cache is unreadable", resolution.reason)
    }

    private class FakeAudioFileStore(
        private val cached: Map<String, String> = emptyMap(),
    ) : AudioFileStore {
        override suspend fun find(cacheFileName: String): String? = cached[cacheFileName]

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String): Unit =
            fail("The resolver must never download; that is the prefetcher's job.")
    }

    private class RecordingPrefetcher : AudioPrefetcher {
        val requests = mutableListOf<Pair<String, String>>()

        override suspend fun prefetch(cacheFileName: String, remoteHttpsUrl: String) {
            requests += cacheFileName to remoteHttpsUrl
        }

        override fun close() = Unit
    }

    private companion object {
        const val CACHE_FILE_NAME = "heavy-rain-v1.mp3"
        const val REMOTE_URL = "https://example.test/heavy-rain.mp3"
        val TRACK = TrackId("heavy-rain")
        val CATALOG = FakeAudioSourceCatalog(
            mapOf(TRACK to TrackSource(CACHE_FILE_NAME, REMOTE_URL)),
        )
    }
}
