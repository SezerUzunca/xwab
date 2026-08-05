package com.xwab.app.core.audiocontent.resolution

import com.xwab.app.core.audiocontent.cache.AudioFileStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        val resolver = LocalFirstAudioContentResolver(FakeAudioFileStore(), prefetcher)

        val resolvedUri = assertNotNull(resolver.resolve("heavy-rain"))

        assertTrue(resolvedUri.startsWith("https://"))
        assertEquals(listOf("heavy-rain-v1.mp3" to resolvedUri), prefetcher.requests)
    }

    @Test
    fun anAlreadyCachedTrackResolvesToTheLocalFileAndIsNotQueuedAgain() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val fileStore = FakeAudioFileStore(
            cached = mapOf("heavy-rain-v1.mp3" to "file:///audio/heavy-rain-v1.mp3"),
        )
        val resolver = LocalFirstAudioContentResolver(fileStore, prefetcher)

        val resolvedUri = assertNotNull(resolver.resolve("heavy-rain"))

        assertEquals("file:///audio/heavy-rain-v1.mp3", resolvedUri)
        assertTrue(prefetcher.requests.isEmpty())
    }

    @Test
    fun unknownContentDoesNotStartPlaybackOrPrefetching() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val resolver = LocalFirstAudioContentResolver(FakeAudioFileStore(), prefetcher)

        assertNull(resolver.resolve("no-such-track"))

        assertTrue(prefetcher.requests.isEmpty())
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
}
