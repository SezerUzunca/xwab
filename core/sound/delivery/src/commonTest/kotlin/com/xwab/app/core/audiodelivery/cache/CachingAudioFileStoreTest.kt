package com.xwab.app.core.audiodelivery.cache

import com.xwab.app.core.audiodelivery.catalogKeeping
import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.network.NetworkClient
import com.xwab.app.core.network.NetworkResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem

/** Cache behaviour against Okio's multiplatform in-memory file system. */
class CachingAudioFileStoreTest {
    private val fileSystem = FakeFileSystem()

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

        assertEquals("/cache/$FILE_NAME", store().find(FILE_NAME))
    }

    @Test
    fun anAbsentFileIsACacheMiss() = runBlocking {
        assertNull(store().find(FILE_NAME))
    }

    @Test
    fun aDirectoryIsNotMistakenForPlayableAudio() = runBlocking {
        fileSystem.createDirectories(ROOT / FILE_NAME)

        assertNull(store().find(FILE_NAME))
    }

    @Test
    fun anEmptyFileIsDiscardedRatherThanServed() = runBlocking {
        writeFile(FILE_NAME, byteArrayOf())

        assertNull(store().find(FILE_NAME))
        assertNull(fileSystem.metadataOrNull(ROOT / FILE_NAME))
    }

    @Test
    fun anAlreadyCachedFileIsNotFetchedAgain() = runBlocking {
        writeFile(FILE_NAME, byteArrayOf(1))
        val network = FakeNetworkClient()

        store(network).download(FILE_NAME, REMOTE_URL)

        assertEquals(0, network.downloads)
        assertContentEquals(byteArrayOf(1), readFile(FILE_NAME))
    }

    @Test
    fun aFetchedFileIsStagedAndThenAtomicallyPromoted() = runBlocking {
        val body = byteArrayOf(1, 2, 3, 4)
        val network = FakeNetworkClient(body = body)

        store(network).download(FILE_NAME, REMOTE_URL)

        assertEquals(1, network.downloads)
        assertContentEquals(body, readFile(FILE_NAME))
        assertNull(fileSystem.metadataOrNull(ROOT / partialCacheFileName(FILE_NAME)))
    }

    @Test
    fun aCompletedDownloadClearsFilesTheCatalogNoLongerRefersTo() = runBlocking {
        val kept = "calm-waves-v1.mp3"
        writeFile(kept, byteArrayOf(1))
        writeFile("long-gone-v1.mp3", byteArrayOf(2))

        store(sourceCatalog = catalogKeeping(FILE_NAME, kept)).download(FILE_NAME, REMOTE_URL)

        assertNull(fileSystem.metadataOrNull(ROOT / "long-gone-v1.mp3"))
        assertContentEquals(byteArrayOf(1), readFile(kept))
    }

    @Test
    fun aNetworkFailureLeavesNoStagedFileBehind() = runBlocking {
        val network = FakeNetworkClient(failure = IllegalStateException("host unreachable"))

        assertFailsWith<IllegalStateException> { store(network).download(FILE_NAME, REMOTE_URL) }

        assertTrue(fileSystem.listOrNull(ROOT).orEmpty().isEmpty())
    }

    @Test
    fun anEmptyTransferIsRefusedAndNotPromoted() = runBlocking {
        val network = FakeNetworkClient(body = byteArrayOf(), contentLength = 0L)

        assertFailsWith<IllegalStateException> { store(network).download(FILE_NAME, REMOTE_URL) }

        assertTrue(fileSystem.listOrNull(ROOT).orEmpty().isEmpty())
    }

    @Test
    fun aCancelledTransferStillClearsWhatItStaged() = runBlocking {
        val staged = CompletableDeferred<Unit>()
        val network = FakeNetworkClient(
            body = byteArrayOf(1, 2, 3),
            afterChunk = {
                staged.complete(Unit)
                awaitCancellation()
            },
        )
        val download = launch(Dispatchers.Default) {
            store(network).download(FILE_NAME, REMOTE_URL)
        }

        staged.await()
        download.cancelAndJoin()

        assertTrue(fileSystem.listOrNull(ROOT).orEmpty().isEmpty())
    }

    @Test
    fun anUnsafeNameNeverReachesTheFileSystemOrNetwork() = runBlocking {
        val network = FakeNetworkClient()

        assertFailsWith<IllegalArgumentException> { store(network).find("../etc/passwd") }
        assertFailsWith<IllegalArgumentException> {
            store(network).download("../etc/passwd", REMOTE_URL)
        }

        assertEquals(0, network.downloads)
        assertTrue(fileSystem.allPaths.isEmpty())
    }

    @Test
    fun downloadingCreatesTheCacheRoot() = runBlocking {
        store().download(FILE_NAME, REMOTE_URL)

        assertTrue(fileSystem.metadata(ROOT).isDirectory)
    }

    private fun store(
        network: NetworkClient = FakeNetworkClient(),
        sourceCatalog: AudioSourceCatalog = catalogKeeping(FILE_NAME),
    ) = CachingAudioFileStore(
        fileSystem = fileSystem,
        root = ROOT,
        network = network,
        sourceCatalog = sourceCatalog,
        fileDispatcher = Dispatchers.Default,
    )

    private fun writeFile(name: String, bytes: ByteArray) {
        fileSystem.createDirectories(ROOT)
        fileSystem.sink(ROOT / name).buffer().use { it.write(bytes) }
    }

    private fun readFile(name: String): ByteArray =
        fileSystem.source(ROOT / name).buffer().use { it.readByteArray() }

    private class FakeNetworkClient(
        private val body: ByteArray = byteArrayOf(1, 2, 3),
        private val contentLength: Long? = body.size.toLong(),
        private val failure: Throwable? = null,
        private val afterChunk: suspend () -> Unit = {},
    ) : NetworkClient {
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
            onResponse(NetworkResponse(200, "audio/mpeg", contentLength))
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
