package com.xwab.app.core.audiocontent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class HybridAudioContentResolverTest {
    @Test
    fun bundledContentResolvesWithoutTouchingTheNetworkStore() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val resolver = HybridAudioContentResolver(fileStore, resourceUri = ::testResourceUri)
        try {
            val resolved = assertNotNull(resolver.resolve("gentle-rain"))

            assertEquals("resource://files/audio/gentle_rain.mp3", resolved.uri)
            assertTrue(resolved.isLocal)
            assertEquals(0, fileStore.downloadCount)
        } finally {
            resolver.close()
        }
    }

    @Test
    fun remoteContentStreamsImmediatelyThenResolvesFromTheDownloadedFile() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val resolver = HybridAudioContentResolver(fileStore, resourceUri = ::testResourceUri)
        try {
            val first = assertNotNull(resolver.resolve("heavy-rain"))

            assertTrue(first.uri.startsWith("https://"))
            assertFalse(first.isLocal)
            withTimeout(2_000) { fileStore.downloaded.await() }

            val second = assertNotNull(resolver.resolve("heavy-rain"))
            assertEquals("file:///audio/heavy-rain-v1.mp3", second.uri)
            assertTrue(second.isLocal)
            assertEquals(1, fileStore.downloadCount)
        } finally {
            resolver.close()
        }
    }

    @Test
    fun repeatedRemoteResolutionsShareOneInFlightDownload() = runBlocking {
        val fileStore = BlockingAudioFileStore()
        val resolver = HybridAudioContentResolver(fileStore, resourceUri = ::testResourceUri)
        try {
            resolver.resolve("heavy-rain")
            withTimeout(2_000) { fileStore.downloadStarted.await() }

            resolver.resolve("heavy-rain")
            assertEquals(1, fileStore.downloadCount)

            fileStore.releaseDownload.complete(Unit)
            withTimeout(2_000) { fileStore.downloadFinished.await() }
            assertEquals(1, fileStore.downloadCount)
        } finally {
            resolver.close()
        }
    }

    @Test
    fun unknownContentDoesNotStartPlaybackOrDownload() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val resolver = HybridAudioContentResolver(fileStore, resourceUri = ::testResourceUri)
        try {
            assertNull(resolver.resolve("no-such-track"))
            assertEquals(0, fileStore.downloadCount)
        } finally {
            resolver.close()
        }
    }

    private class FakeAudioFileStore : AudioFileStore {
        private val cached = mutableMapOf<String, String>()
        val downloaded = CompletableDeferred<Unit>()
        var downloadCount = 0

        override suspend fun find(cacheFileName: String): String? = cached[cacheFileName]

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
            downloadCount++
            cached[cacheFileName] = "file:///audio/$cacheFileName"
            downloaded.complete(Unit)
        }
    }

    private class BlockingAudioFileStore : AudioFileStore {
        val downloadStarted = CompletableDeferred<Unit>()
        val releaseDownload = CompletableDeferred<Unit>()
        val downloadFinished = CompletableDeferred<Unit>()
        var downloadCount = 0

        override suspend fun find(cacheFileName: String): String? = null

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
            downloadCount++
            downloadStarted.complete(Unit)
            releaseDownload.await()
            downloadFinished.complete(Unit)
        }
    }

    private fun testResourceUri(path: String): String = "resource://$path"
}
