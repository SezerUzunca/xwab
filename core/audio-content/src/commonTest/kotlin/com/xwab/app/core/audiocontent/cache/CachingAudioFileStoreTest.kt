package com.xwab.app.core.audiocontent.cache

import com.xwab.app.core.audiocontent.catalog.catalogCacheFileNames
import com.xwab.app.core.audiocontent.catalog.partialCacheFileName
import kotlin.test.Test
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
import kotlinx.coroutines.withContext

/**
 * The cache's behaviour, which used to be untestable: both stores reached for a socket and a real
 * file system from inside the method under test, so nothing below could be asserted at all.
 */
class CachingAudioFileStoreTest {
    @Test
    fun aCachedFileIsAnsweredWithItsLocalUri() = runBlocking {
        val directory = FakeCacheDirectory(files = mutableMapOf(FILE_NAME to 1_024L))

        assertEquals("file:///cache/$FILE_NAME", store(directory).find(FILE_NAME))
    }

    @Test
    fun anAbsentFileIsACacheMiss() = runBlocking {
        assertNull(store().find(FILE_NAME))
    }

    /** A zero-length file is what a failed write leaves behind, and it would never play. */
    @Test
    fun anEmptyFileIsDiscardedRatherThanServed() = runBlocking {
        val directory = FakeCacheDirectory(files = mutableMapOf(FILE_NAME to 0L))

        assertNull(store(directory).find(FILE_NAME))
        assertTrue(FILE_NAME !in directory.files)
    }

    @Test
    fun anAlreadyCachedFileIsNotFetchedAgain() = runBlocking {
        val directory = FakeCacheDirectory(files = mutableMapOf(FILE_NAME to 1_024L))
        val transport = FakeTransport(directory)

        store(directory, transport).download(FILE_NAME, REMOTE_URL)

        assertEquals(0, transport.fetches)
        assertEquals(1_024L, directory.files[FILE_NAME])
    }

    @Test
    fun aFetchedFileIsStagedAndThenPromoted() = runBlocking {
        val directory = FakeCacheDirectory()
        val transport = FakeTransport(directory, bytes = 2_048L)

        store(directory, transport).download(FILE_NAME, REMOTE_URL)

        assertEquals(1, transport.fetches)
        assertEquals(listOf(partialCacheFileName(FILE_NAME)), transport.stagedUnder)
        assertEquals(2_048L, directory.files[FILE_NAME])
        assertTrue(partialCacheFileName(FILE_NAME) !in directory.files, "the staged file should be gone")
    }

    /** The sweep runs on the one event that is known to have changed the cache. */
    @Test
    fun aCompletedDownloadClearsFilesTheCatalogNoLongerRefersTo() = runBlocking {
        val kept = catalogCacheFileNames.first()
        val directory = FakeCacheDirectory(
            files = mutableMapOf(kept to 512L, "long-gone-v1.mp3" to 512L),
        )

        store(directory, FakeTransport(directory)).download(FILE_NAME, REMOTE_URL)

        assertTrue("long-gone-v1.mp3" !in directory.files)
        assertEquals(512L, directory.files[kept], "a referenced track should survive the sweep")
    }

    @Test
    fun aTransportFailureLeavesNoStagedFileBehind() = runBlocking {
        val directory = FakeCacheDirectory()
        val transport = FakeTransport(directory, failure = { error("The audio host is unreachable.") })

        assertFailsWith<IllegalStateException> { store(directory, transport).download(FILE_NAME, REMOTE_URL) }
        assertTrue(directory.files.isEmpty(), "a partial transfer should not outlive its failure")
    }

    @Test
    fun aTransferThatLandedEmptyIsRefusedAndNotPromoted() = runBlocking {
        val directory = FakeCacheDirectory()
        val transport = FakeTransport(directory, bytes = 0L)

        assertFailsWith<IllegalStateException> { store(directory, transport).download(FILE_NAME, REMOTE_URL) }
        assertTrue(directory.files.isEmpty())
    }

    /** Measured again as it lands, because a source may declare a length it does not honour. */
    @Test
    fun aTransferOverTheSizeLimitIsUnusableRatherThanRetried() = runBlocking {
        val directory = FakeCacheDirectory()
        val transport = FakeTransport(directory, bytes = MAX_DOWNLOAD_BYTES + 1)

        assertFailsWith<UnusableAudioSourceException> {
            store(directory, transport).download(FILE_NAME, REMOTE_URL)
        }
        assertTrue(directory.files.isEmpty())
    }

    /**
     * A staged file is the one thing nothing else would ever clear: the sweep skips partial names
     * by design, so a transfer cancelled at close would leave its bytes on disk for good.
     */
    @Test
    fun aCancelledTransferStillClearsWhatItStaged() = runBlocking {
        val directory = FakeCacheDirectory()
        val staged = CompletableDeferred<Unit>()
        val transport = object : AudioTransport {
            override suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String) {
                directory.files[partialFileName] = 512L
                staged.complete(Unit)
                awaitCancellation()
            }
        }

        val download = launch(Dispatchers.Default) {
            CachingAudioFileStore(directory, transport).download(FILE_NAME, REMOTE_URL)
        }
        staged.await()
        download.cancelAndJoin()

        assertTrue(directory.files.isEmpty(), "a cancelled transfer should not leave a staged file")
    }

    @Test
    fun aPlainHttpSourceIsRefusedBeforeAnythingIsOpened() = runBlocking {
        val transport = FakeTransport(FakeCacheDirectory())

        assertFailsWith<IllegalArgumentException> {
            store(transport = transport).download(FILE_NAME, "http://example.test/heavy-rain.mp3")
        }
        assertEquals(0, transport.fetches)
    }

    @Test
    fun anUnsafeNameNeverReachesTheFileSystem() = runBlocking {
        val directory = FakeCacheDirectory()

        assertFailsWith<IllegalArgumentException> { store(directory).find("../etc/passwd") }
        assertFailsWith<IllegalArgumentException> { store(directory).download("../etc/passwd", REMOTE_URL) }
        assertTrue(directory.touched.isEmpty(), "the directory should not have been asked anything")
    }

    private fun store(
        directory: FakeCacheDirectory = FakeCacheDirectory(),
        transport: AudioTransport = FakeTransport(directory),
    ) = CachingAudioFileStore(directory, transport)

    /** An in-memory cache root: names to sizes, with every call it was asked to make recorded. */
    private class FakeCacheDirectory(
        val files: MutableMap<String, Long> = mutableMapOf(),
    ) : AudioCacheDirectory {
        val touched = mutableListOf<String>()

        override suspend fun prepare() {
            touched += "prepare"
        }

        override suspend fun sizeOf(fileName: String): Long? {
            touched += "sizeOf:$fileName"
            return files[fileName]
        }

        /**
         * Dispatches like the real adapters do. Without that this fake would happily delete on a
         * cancelled coroutine — and a cleanup that skips cancellation would go unnoticed.
         */
        override suspend fun delete(fileName: String) {
            withContext(Dispatchers.Default) {
                touched += "delete:$fileName"
                files.remove(fileName)
            }
        }

        override fun localUri(fileName: String): String = "file:///cache/$fileName"

        override suspend fun list(): List<String> = files.keys.toList()

        override suspend fun promote(partialFileName: String, cacheFileName: String) {
            val bytes = requireNotNull(files.remove(partialFileName)) { "nothing staged to promote" }
            files[cacheFileName] = bytes
        }
    }

    private class FakeTransport(
        private val directory: FakeCacheDirectory,
        private val bytes: Long = 1_024L,
        private val failure: (() -> Nothing)? = null,
    ) : AudioTransport {
        val stagedUnder = mutableListOf<String>()
        var fetches = 0

        override suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String) {
            fetches++
            stagedUnder += partialFileName
            // A failing transfer still leaves bytes on disk, which is what the store cleans up.
            directory.files[partialFileName] = bytes
            failure?.invoke()
        }
    }

    private companion object {
        const val FILE_NAME = "heavy-rain-v1.mp3"
        const val REMOTE_URL = "https://example.test/heavy-rain.mp3"
    }
}
