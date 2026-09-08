package com.xwab.app.core.sounddelivery.resolution

import com.xwab.app.core.sounddelivery.FakeSoundPort
import com.xwab.app.core.sounddelivery.cache.AudioFileStore
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sounddelivery.port.SoundContentResolution
import com.xwab.app.core.sound.port.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

class LocalFirstSoundContentAdapterTest {
    @Test
    fun anUncachedTrackStreamsAndIsQueuedForLater() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val adapter = LocalFirstSoundContentAdapter(FakeAudioFileStore(), prefetcher, SOURCE_PORT)

        val resolution = assertIs<SoundContentResolution.Resolved>(adapter.resolve(TRACK))

        assertEquals(REMOTE_URL, resolution.uri)
        assertEquals(listOf(CACHE_FILE_NAME to REMOTE_URL), prefetcher.requests)
    }

    @Test
    fun anAlreadyCachedTrackResolvesToTheLocalFileAndIsNotQueuedAgain() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val fileStore = FakeAudioFileStore(
            cached = mapOf(CACHE_FILE_NAME to "/audio/$CACHE_FILE_NAME"),
        )
        val adapter = LocalFirstSoundContentAdapter(fileStore, prefetcher, SOURCE_PORT)

        val resolution = assertIs<SoundContentResolution.Resolved>(adapter.resolve(TRACK))

        assertEquals("/audio/$CACHE_FILE_NAME", resolution.uri)
        assertTrue(prefetcher.requests.isEmpty())
    }

    @Test
    fun unknownContentIsNotFoundAndStartsNoPrefetch() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val adapter = LocalFirstSoundContentAdapter(FakeAudioFileStore(), prefetcher, SOURCE_PORT)

        assertEquals(SoundContentResolution.NotFound, adapter.resolve(TrackId("no-such-track")))
        assertTrue(prefetcher.requests.isEmpty())
    }

    @Test
    fun aLookupThatFailsOutrightIsUnavailableRatherThanAnEscapingError() = runBlocking {
        val adapter = LocalFirstSoundContentAdapter(
            object : AudioFileStore {
                override suspend fun find(cacheFileName: String): String? =
                    error("the cache is unreadable")

                override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) = Unit
            },
            RecordingPrefetcher(),
            SOURCE_PORT,
        )

        val resolution = assertIs<SoundContentResolution.Unavailable>(adapter.resolve(TRACK))

        assertEquals("the cache is unreadable", resolution.reason)
    }

    private class FakeAudioFileStore(
        private val cached: Map<String, String> = emptyMap(),
    ) : AudioFileStore {
        override suspend fun find(cacheFileName: String): String? = cached[cacheFileName]

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String): Unit =
            fail("The adapter must never download; that is the prefetcher's job.")
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
        val SOURCE_PORT = FakeSoundPort(
            mapOf(TRACK to TrackSource(CACHE_FILE_NAME, REMOTE_URL)),
        )
    }
}
