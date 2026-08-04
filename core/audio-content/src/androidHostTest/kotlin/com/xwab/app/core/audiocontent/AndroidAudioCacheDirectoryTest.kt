package com.xwab.app.core.audiocontent

import java.io.File
import java.net.URI
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * The adapter against a real directory, which is the half [CachingAudioFileStoreTest] has to take
 * on trust. A fake can agree with the store about what a cache hit is; only the file system can say
 * whether `File` agrees too.
 */
class AndroidAudioCacheDirectoryTest {
    private lateinit var root: File
    private lateinit var directory: AndroidAudioCacheDirectory

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("audio-content").toFile()
        directory = AndroidAudioCacheDirectory(root)
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aMissingFileHasNoSize() = runBlocking {
        assertNull(directory.sizeOf(FILE_NAME))
    }

    @Test
    fun aWrittenFileReportsItsLength() = runBlocking {
        write(FILE_NAME, bytes = 3)

        assertEquals(3L, directory.sizeOf(FILE_NAME))
    }

    /** Regular files only: a directory answering with a size would read as a cache hit. */
    @Test
    fun aDirectoryIsNotACachedFile() = runBlocking {
        File(root, FILE_NAME).mkdirs()

        assertNull(directory.sizeOf(FILE_NAME))
    }

    /** The store deletes staged names it may never have created, so this cannot be an error. */
    @Test
    fun deletingWhatIsNotThereIsSilent() = runBlocking {
        directory.delete(FILE_NAME)

        write(FILE_NAME, bytes = 1)
        directory.delete(FILE_NAME)
        assertNull(directory.sizeOf(FILE_NAME))
    }

    @Test
    fun theLocalUriRoundTripsToTheFileOnDisk() = runBlocking {
        write(FILE_NAME, bytes = 1)

        val uri = directory.localUri(FILE_NAME)

        assertTrue(uri.startsWith("file:"), uri)
        assertEquals(File(root, FILE_NAME).canonicalFile, File(URI(uri)).canonicalFile)
    }

    /** The sweep reads this listing, so a staged transfer has to be visible in it. */
    @Test
    fun theListingIncludesStagedTransfers() = runBlocking {
        write(FILE_NAME, bytes = 1)
        write(partialCacheFileName(FILE_NAME), bytes = 1)

        assertEquals(setOf(FILE_NAME, partialCacheFileName(FILE_NAME)), directory.list().toSet())
    }

    @Test
    fun promotingMovesTheStagedTransferOntoItsFinalName() = runBlocking {
        write(partialCacheFileName(FILE_NAME), bytes = 5)

        directory.promote(partialCacheFileName(FILE_NAME), FILE_NAME)

        assertEquals(5L, directory.sizeOf(FILE_NAME))
        assertNull(directory.sizeOf(partialCacheFileName(FILE_NAME)))
    }

    /** A silent failure here would leave the caller believing a track was cached. */
    @Test
    fun promotingNothingFails() = runBlocking {
        assertFailsWith<IllegalStateException> {
            directory.promote(partialCacheFileName(FILE_NAME), FILE_NAME)
        }
    }

    @Test
    fun prepareCreatesTheCacheRootWhenItIsMissing() = runBlocking {
        val missing = File(root, "nested/audio-content")

        AndroidAudioCacheDirectory(missing).prepare()

        assertTrue(missing.isDirectory)
    }

    /**
     * The whole flow over a real directory. The sweep in particular reads `File.list()`, which no
     * fake can stand in for: it is the one place where a naming rule meets an actual disk.
     */
    @Test
    fun theStoreCachesPromotesAndSweepsOnDisk() = runBlocking {
        val cached = catalogCacheFileNames.first()
        write("long-gone-v1.mp3", bytes = 4)
        val store = CachingAudioFileStore(directory, TransportWritingBytes(root, bytes = 8))

        store.download(cached, REMOTE_URL)

        assertEquals(8L, directory.sizeOf(cached))
        assertNull(directory.sizeOf("long-gone-v1.mp3"), "a track that left the catalog should be gone")
        assertTrue(directory.list().none { it.startsWith(".") }, "no staged transfer should survive")
        assertEquals(File(root, cached).canonicalFile, File(URI(store.find(cached)!!)).canonicalFile)
    }

    /** An interrupted write leaves a zero-length file, which would otherwise never play. */
    @Test
    fun anEmptyCachedFileIsDiscardedOnLookup() = runBlocking {
        write(FILE_NAME, bytes = 0)
        val store = CachingAudioFileStore(directory, TransportWritingBytes(root, bytes = 1))

        assertNull(store.find(FILE_NAME))
        assertNull(directory.sizeOf(FILE_NAME))
    }

    private fun write(fileName: String, bytes: Int) {
        File(root, fileName).writeBytes(ByteArray(bytes))
    }

    /** Stands in for the network only: everything downstream of it is the real thing. */
    private class TransportWritingBytes(
        private val root: File,
        private val bytes: Int,
    ) : AudioTransport {
        override suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String) {
            File(root, partialFileName).writeBytes(ByteArray(bytes))
        }
    }

    private companion object {
        const val FILE_NAME = "heavy-rain-v1.mp3"
        const val REMOTE_URL = "https://example.test/heavy-rain.mp3"
    }
}
