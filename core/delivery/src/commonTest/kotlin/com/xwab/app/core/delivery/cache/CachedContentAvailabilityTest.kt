package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.FileMetadata
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/** The UI must never confuse a stream or partially written download with offline availability. */
class CachedContentAvailabilityTest {
    private val fileSystem = FakeFileSystem()
    private val key = CacheKey("sound", "rain-v1.mp3")
    private val request = DeliveryRequest(key, "https://example.test/rain.mp3")
    private val root = "/cache".toPath()

    @AfterTest
    fun closeFileSystem() {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }

    @Test
    fun observationChecksDiskWithoutStartingDownloadsOrCreatingFiles() = runBlocking {
        val network = RecordingNetwork()
        val store = store(network)

        assertFalse(store.observeCached(key).first())
        assertEquals(0, network.downloads)
        assertTrue(fileSystem.allPaths.isEmpty())

        write(key, byteArrayOf(1))
        assertTrue(store.observeCached(key).first())
        fileSystem.delete(root / key.namespace / key.fileName)
        assertFalse(store.observeCached(key).first())
        assertEquals(0, network.downloads)
    }

    @Test
    fun aSubscriberBecomesReadyOnlyAfterAtomicCompletion() = runBlocking {
        withTimeout(TEST_TIMEOUT_MS) {
            val staged = CompletableDeferred<Unit>()
            val finishDownload = CompletableDeferred<Unit>()
            val store = store(RecordingNetwork(afterChunk = {
                staged.complete(Unit)
                finishDownload.await()
            }))
            val changes = Channel<Boolean>(Channel.UNLIMITED)
            val observer = launch { store.observeCached(key).collect { changes.send(it) } }
            try {
                assertFalse(changes.receive())
                val download = launch { store.download(request) }
                staged.await()
                assertFalse(store.observeCached(key).first())
                assertTrue(changes.tryReceive().isFailure)

                finishDownload.complete(Unit)
                download.join()
                assertTrue(changes.receive())
            } finally {
                observer.cancelAndJoin()
            }
        }
    }

    @Test
    fun failedDownloadsNeverReportOfflineReady() = runBlocking {
        val store = store(RecordingNetwork(afterChunk = { error("connection lost") }))
        assertFalse(store.observeCached(key).first())

        assertFailsWith<IllegalStateException> { store.download(request) }

        assertFalse(store.observeCached(key).first())
        assertFalse(fileSystem.exists(root / key.namespace / key.fileName))
    }

    @Test
    fun inventorySweepsUpdateExistingSubscribers() = runBlocking {
        withTimeout(TEST_TIMEOUT_MS) {
            val oldKey = CacheKey("sound", "old-rain-v1.mp3")
            write(oldKey, byteArrayOf(1))
            val store = store()
            val oldChanges = Channel<Boolean>(Channel.UNLIMITED)
            val oldObserver = launch { store.observeCached(oldKey).collect { oldChanges.send(it) } }
            val currentChanges = Channel<Boolean>(Channel.UNLIMITED)
            val currentObserver = launch { store.observeCached(key).collect { currentChanges.send(it) } }
            try {
                assertTrue(oldChanges.receive())
                assertFalse(currentChanges.receive())

                store.download(request.copy(retainedFileNames = setOf(key.fileName)))
                assertFalse(oldChanges.receive())
                assertTrue(currentChanges.receive())
            } finally {
                oldObserver.cancelAndJoin()
                currentObserver.cancelAndJoin()
            }
        }
    }

    @Test
    fun emptyFilesAndDirectoriesAreNotOfflineReady() = runBlocking {
        write(key, byteArrayOf())
        assertFalse(store().observeCached(key).first())
        // Observation is read-only, including malformed cache entries.
        assertTrue(fileSystem.exists(root / key.namespace / key.fileName))
        fileSystem.delete(root / key.namespace / key.fileName)
        fileSystem.createDirectories(root / key.namespace / key.fileName)
        assertFalse(store().observeCached(key).first())
    }

    @Test
    fun anUnreadableCacheReportsFalseInsteadOfClaimingAvailability() = runBlocking {
        write(key, byteArrayOf(1))
        val unreadable = object : ForwardingFileSystem(fileSystem) {
            override fun metadataOrNull(path: Path): FileMetadata? = throw IOException("unreadable")
        }

        assertFalse(store(fileSystem = unreadable).observeCached(key).first())
    }

    // Inject Unconfined deliberately so this in-memory filesystem stays on the collecting test thread.
    @Suppress("InjectDispatcher")
    private fun store(
        network: NetworkPort = RecordingNetwork(),
        fileSystem: FileSystem = this.fileSystem,
    ) = CachingContentFileStore(fileSystem, root, network, Dispatchers.Unconfined)

    private fun write(key: CacheKey, bytes: ByteArray) {
        fileSystem.createDirectories(root / key.namespace)
        fileSystem.write(root / key.namespace / key.fileName) { write(bytes) }
    }

    private class RecordingNetwork(private val afterChunk: suspend () -> Unit = {}) : NetworkPort {
        var downloads = 0

        override suspend fun download(
            httpsUrl: String,
            headers: Map<String, String>,
            onResponse: (NetworkResponse) -> Unit,
            onChunk: (ByteArray, Int) -> Unit,
        ) {
            downloads++
            onResponse(NetworkResponse(200, "audio/mpeg", 1L))
            onChunk(byteArrayOf(1), 1)
            afterChunk()
        }
    }

    private companion object {
        const val TEST_TIMEOUT_MS = 5_000L
    }
}
