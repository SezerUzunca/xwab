package com.xwab.app.core.audiocontent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class HybridAudioContentRepositoryTest {
    @Test
    fun everyCategoryHasThreeOrMoreTracksAndAnAccurateCount() = runBlocking {
        val repository = HybridAudioContentRepository(FakeAudioFileStore(), resourceUri = ::testResourceUri)
        try {
            repository.observeCategories().first().forEach { category ->
                val music = repository.observeMusicForCategory(category.id).first()
                assertTrue(music.size >= 3, "${category.id} should have at least three tracks")
                assertEquals(music.size, category.musicCount)
                assertTrue(music.all { it.categoryId == category.id })
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun catalogIdsSourcesAndCategoryReferencesAreValidAndUnique() = runBlocking {
        val musicIds = catalogEntries.map { it.music.id }
        val categoryIds = catalogCategories.map { it.id }.toSet()

        assertEquals(musicIds.size, musicIds.toSet().size, "duplicate music ids")
        assertTrue(catalogEntries.all { it.music.categoryId in categoryIds })

        val bundledPaths = catalogEntries.mapNotNull {
            (it.asset as? AudioAssetSpec.Bundled)?.resourcePath
        }
        val remoteUrls = catalogEntries.mapNotNull {
            (it.asset as? AudioAssetSpec.Remote)?.httpsUrl
        }
        assertEquals(5, bundledPaths.size)
        assertEquals(10, remoteUrls.size)
        assertEquals(bundledPaths.size, bundledPaths.toSet().size)
        assertEquals(remoteUrls.size, remoteUrls.toSet().size)
        assertTrue(remoteUrls.all { it.startsWith("https://") && it.endsWith(".mp3") })
    }

    @Test
    fun bundledContentResolvesWithoutTouchingTheNetworkStore() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val repository = HybridAudioContentRepository(fileStore, resourceUri = ::testResourceUri)
        try {
            val resolved = assertNotNull(repository.resolve("gentle-rain"))

            assertEquals("resource://files/audio/gentle_rain.mp3", resolved.uri)
            assertTrue(resolved.isLocal)
            assertEquals(0, fileStore.downloadCount)
        } finally {
            repository.close()
        }
    }

    @Test
    fun remoteContentStreamsImmediatelyThenResolvesFromTheDownloadedFile() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val repository = HybridAudioContentRepository(fileStore, resourceUri = ::testResourceUri)
        try {
            val first = assertNotNull(repository.resolve("heavy-rain"))

            assertTrue(first.uri.startsWith("https://"))
            assertFalse(first.isLocal)
            withTimeout(2_000) { fileStore.downloaded.await() }

            val second = assertNotNull(repository.resolve("heavy-rain"))
            assertEquals("file:///audio/heavy-rain-v1.mp3", second.uri)
            assertTrue(second.isLocal)
            assertEquals(1, fileStore.downloadCount)
        } finally {
            repository.close()
        }
    }

    @Test
    fun repeatedRemoteResolutionsShareOneInFlightDownload() = runBlocking {
        val fileStore = BlockingAudioFileStore()
        val repository = HybridAudioContentRepository(fileStore, resourceUri = ::testResourceUri)
        try {
            repository.resolve("heavy-rain")
            withTimeout(2_000) { fileStore.downloadStarted.await() }

            repository.resolve("heavy-rain")
            assertEquals(1, fileStore.downloadCount)

            fileStore.releaseDownload.complete(Unit)
            withTimeout(2_000) { fileStore.downloadFinished.await() }
            assertEquals(1, fileStore.downloadCount)
        } finally {
            repository.close()
        }
    }

    @Test
    fun unknownContentDoesNotStartPlaybackOrDownload() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val repository = HybridAudioContentRepository(fileStore, resourceUri = ::testResourceUri)
        try {
            assertNull(repository.resolve("no-such-track"))
            assertEquals(0, fileStore.downloadCount)
        } finally {
            repository.close()
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
