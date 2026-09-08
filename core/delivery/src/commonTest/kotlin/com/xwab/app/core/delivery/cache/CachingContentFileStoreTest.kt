package com.xwab.app.core.delivery.cache

import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
// `okio.use` (not the stdlib's) is required on Kotlin/Native — see the note on `writeDownload` in
// CachingContentFileStore.kt.
import okio.use

/** Cache behaviour against Okio's multiplatform in-memory file system. */
class CachingContentFileStoreTest {
    private val fileSystem = FakeFileSystem()

    @Test
    fun namespacesSeparateCachedBytesAndInventoryCleanup() = runBlocking {
        val sound = DeliveryRequest(CacheKey("sound", "item-v1.mp3"), REMOTE_URL)
        val story = sound.copy(key = CacheKey("story", "item-v1.mp3"))
        store(FakeNetworkPort(body = byteArrayOf(1))).download(sound)
        store(FakeNetworkPort(body = byteArrayOf(2))).download(story)
        store().download(story.copy(
            key = CacheKey("story", "item-v2.mp3"),
            retainedFileNames = setOf("item-v2.mp3"),
        ))

        assertEquals("/cache/sound/item-v1.mp3", store().find(sound.key))
        assertNull(store().find(story.key))
        assertContentEquals(byteArrayOf(1), fileSystem.read(ROOT / "sound" / "item-v1.mp3") { readByteArray() })
    }

    @Test
    fun documentsUseTheirOwnTypeLimitAndFileExtension() = runBlocking {
        val document = DeliveryRequest(
            CacheKey("documents", "guide-v1.pdf"), "https://example.test/guide.pdf",
            acceptedContentTypes = setOf("application/pdf"), maxBytes = 1024,
        )
        store(FakeNetworkPort(contentType = "application/pdf")).download(document)
        assertEquals("/cache/documents/guide-v1.pdf", store().find(document.key))
    }

    @AfterTest
    fun closeFileSystem() {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }

    /**
     * The leading slash is load-bearing, not cosmetic: `IosPlaybackEngine` decides between
     * `NSURL.fileURLWithPath` and `NSURL.URLWithString` by testing for it, and Media3 treats a
     * scheme-less URI as a local file. A cache hit that stopped being an absolute path would reach
     * both platforms as a malformed remote URL.
     */
    @Test
    fun aCachedFileIsAnsweredWithItsAbsolutePath() = runBlocking {
        writeFile(FILE_NAME, byteArrayOf(1, 2, 3))

        assertEquals("/cache/sample/$FILE_NAME", store().find(CacheKey("sample", FILE_NAME)))
    }

    @Test
    fun anAbsentFileIsACacheMiss() = runBlocking {
        assertNull(store().find(CacheKey("sample", FILE_NAME)))
    }

    @Test
    fun aDirectoryIsNotMistakenForPlayableAudio() = runBlocking {
        fileSystem.createDirectories(ROOT / "sample" / FILE_NAME)

        assertNull(store().find(CacheKey("sample", FILE_NAME)))
    }

    @Test
    fun anEmptyFileIsDiscardedRatherThanServed() = runBlocking {
        writeFile(FILE_NAME, byteArrayOf())

        assertNull(store().find(CacheKey("sample", FILE_NAME)))
        assertNull(fileSystem.metadataOrNull(ROOT / "sample" / FILE_NAME))
    }

    @Test
    fun anAlreadyCachedFileIsNotFetchedAgain() = runBlocking {
        writeFile(FILE_NAME, byteArrayOf(1))
        val network = FakeNetworkPort()

        store(network).download(request())

        assertEquals(0, network.downloads)
        assertContentEquals(byteArrayOf(1), readFile(FILE_NAME))
    }

    @Test
    fun aFetchedFileIsStagedAndThenAtomicallyPromoted() = runBlocking {
        val body = byteArrayOf(1, 2, 3, 4)
        val network = FakeNetworkPort(body = body)

        store(network).download(request())

        assertEquals(1, network.downloads)
        assertContentEquals(body, readFile(FILE_NAME))
        assertNull(fileSystem.metadataOrNull(ROOT / "sample" / partialCacheFileName(FILE_NAME)))
    }

    @Test
    fun aCompletedDownloadClearsFilesTheCatalogNoLongerRefersTo() = runBlocking {
        val kept = "calm-waves-v1.mp3"
        writeFile(kept, byteArrayOf(1))
        writeFile("long-gone-v1.mp3", byteArrayOf(2))

        store().download(request(retained = setOf(FILE_NAME, kept)))

        assertNull(fileSystem.metadataOrNull(ROOT / "sample" / "long-gone-v1.mp3"))
        assertContentEquals(byteArrayOf(1), readFile(kept))
    }

    @Test
    fun aNetworkFailureLeavesNoStagedFileBehind() = runBlocking {
        val network = FakeNetworkPort(failure = IllegalStateException("host unreachable"))

        assertFailsWith<IllegalStateException> { store(network).download(request()) }

        assertTrue(fileSystem.listOrNull(ROOT / "sample").orEmpty().isEmpty())
    }

    @Test
    fun anEmptyTransferIsRefusedAndNotPromoted() = runBlocking {
        val network = FakeNetworkPort(body = byteArrayOf(), contentLength = 0L)

        assertFailsWith<IllegalStateException> { store(network).download(request()) }

        assertTrue(fileSystem.listOrNull(ROOT / "sample").orEmpty().isEmpty())
    }

    @Test
    fun aCancelledTransferStillClearsWhatItStaged() = runBlocking {
        val staged = CompletableDeferred<Unit>()
        val network = FakeNetworkPort(
            body = byteArrayOf(1, 2, 3),
            afterChunk = {
                staged.complete(Unit)
                awaitCancellation()
            },
        )
        val download = launch(Dispatchers.Default) {
            store(network).download(request())
        }

        staged.await()
        download.cancelAndJoin()

        assertTrue(fileSystem.listOrNull(ROOT / "sample").orEmpty().isEmpty())
    }

    @Test
    fun anUnsafeNameNeverReachesTheFileSystemOrNetwork() = runBlocking {
        val network = FakeNetworkPort()

        assertFailsWith<IllegalArgumentException> { store(network).find(CacheKey("sample", "../etc/passwd")) }
        assertFailsWith<IllegalArgumentException> {
            store(network).download(DeliveryRequest(CacheKey("sample", "../etc/passwd"), REMOTE_URL))
        }

        assertEquals(0, network.downloads)
        assertTrue(fileSystem.allPaths.isEmpty())
    }

    @Test
    fun downloadingCreatesTheCacheRoot() = runBlocking {
        store().download(request())

        assertTrue(fileSystem.metadata(ROOT).isDirectory)
    }

    /**
     * The pre-namespace cache sat beside the namespace directories rather than inside one, so no
     * sweep can reach it. A download after the upgrade is the only moment that still knows it was
     * there — and a missing legacy directory, the normal case, must not disturb anything.
     */
    @Test
    fun theCacheWrittenBeforeNamespacesIsDroppedOnTheFirstDownload() = runBlocking {
        val legacy = "/legacy".toPath()
        fileSystem.createDirectories(legacy)
        fileSystem.sink(legacy / FILE_NAME).buffer().use { it.write(byteArrayOf(9)) }

        store(legacyRoots = listOf(legacy, "/never-existed".toPath())).download(request())

        assertFalse(fileSystem.exists(legacy))
        assertContentEquals(byteArrayOf(1, 2, 3), readFile(FILE_NAME))
    }

    private fun request(retained: Set<String>? = null) = DeliveryRequest(
        CacheKey("sample", FILE_NAME), REMOTE_URL, retainedFileNames = retained,
    )

    private fun store(
        network: NetworkPort = FakeNetworkPort(),
        legacyRoots: List<Path> = emptyList(),
    ) = CachingContentFileStore(
        fileSystem = fileSystem,
        root = ROOT,
        networkPort = network,
        fileDispatcher = Dispatchers.Default,
        legacyRoots = legacyRoots,
    )

    private fun writeFile(name: String, bytes: ByteArray) {
        fileSystem.createDirectories(ROOT / "sample")
        fileSystem.sink(ROOT / "sample" / name).buffer().use { it.write(bytes) }
    }

    private fun readFile(name: String): ByteArray =
        fileSystem.source(ROOT / "sample" / name).buffer().use { it.readByteArray() }

    private class FakeNetworkPort(
        private val body: ByteArray = byteArrayOf(1, 2, 3),
        private val contentType: String = "audio/mpeg",
        private val contentLength: Long? = body.size.toLong(),
        private val failure: Throwable? = null,
        private val afterChunk: suspend () -> Unit = {},
    ) : NetworkPort {
        var downloads = 0

        override suspend fun getText(httpsUrl: String, headers: Map<String, String>): String =
            error("not used")

        override suspend fun download(
            httpsUrl: String,
            headers: Map<String, String>,
            onResponse: (NetworkResponse) -> Unit,
            onChunk: (bytes: ByteArray, count: Int) -> Unit,
        ) {
            downloads++
            onResponse(NetworkResponse(200, contentType, contentLength))
            if (body.isNotEmpty()) onChunk(body, body.size)
            failure?.let { throw it }
            afterChunk()
        }
    }

    private companion object {
        val ROOT = "/cache".toPath()
        const val FILE_NAME = "heavy-rain-v1.mp3"
        const val REMOTE_URL = "https://example.test/heavy-rain.mp3"
    }
}
