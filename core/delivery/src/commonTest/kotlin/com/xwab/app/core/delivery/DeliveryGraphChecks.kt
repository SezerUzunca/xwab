package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

/**
 * The cache store is shared by scope alone: nothing hands the prefetcher the adapter's instance.
 * Each platform's test builds its own production graph through [buildGraph] — test doubles swapped
 * in through Metro's dynamic graph — and this checks that a prefetch reaches an open observer.
 */
internal fun prefetchUpdatesTheStoreAlreadyObservedThroughThePort(
    buildGraph: (TestDoubles, NetworkPort) -> DeliveryGraph,
): TestResult = runTest {
    val fileSystem = FakeFileSystem()
    val network = GatedNetwork()
    try {
        val graph = buildGraph(
            TestDoubles(fileSystem, backgroundScope, StandardTestDispatcher(testScheduler)),
            network,
        )
        assertSame(graph.delivery, graph.delivery)
        val request = DeliveryRequest(CacheKey("sound", "rain.mp3"), "https://example.test/rain.mp3")
        // Subscribe before prefetch: a separately constructed store would never invalidate it.
        val cached = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { graph.delivery.observeCached(request.key).first { it } }
        }
        graph.delivery.resolve(request)
        network.release.complete(Unit)
        assertTrue(cached.await())
    } finally {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }
}

/**
 * The file system, scope and dispatcher the store and prefetcher take as optional dependencies,
 * bound to test doubles in the production graph.
 */
@BindingContainer
internal class TestDoubles(
    private val fileSystem: FileSystem,
    private val backgroundScope: CoroutineScope,
    private val fileDispatcher: CoroutineDispatcher,
) {
    @Provides fun fileSystem(): FileSystem = fileSystem

    @Provides fun backgroundScope(): CoroutineScope = backgroundScope

    @Provides fun fileDispatcher(): CoroutineDispatcher = fileDispatcher
}

private class GatedNetwork : NetworkPort {
    val release = CompletableDeferred<Unit>()

    override suspend fun download(
        httpsUrl: String,
        headers: Map<String, String>,
        onResponse: (NetworkResponse) -> Unit,
        onChunk: (bytes: ByteArray, count: Int) -> Unit,
    ) {
        release.await()
        onResponse(NetworkResponse(HTTP_OK, "audio/mpeg", 1))
        onChunk(byteArrayOf(1), 1)
    }
}

private const val HTTP_OK = 200
