package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/**
 * The cache store is shared by scope alone: nothing hands the prefetcher the adapter's instance.
 * This builds the module's own wiring and checks that a prefetch reaches an open observer.
 */
class DeliveryGraphTest {
    @Test
    fun prefetchUpdatesTheStoreAlreadyObservedThroughThePort() = runTest {
        val fileSystem = FakeFileSystem()
        val network = GatedNetwork()
        try {
            val graph = createGraphFactory<DeliveryTestGraph.Factory>().create(
                networkPort = network,
                location = TestCacheLocation,
                fileSystem = fileSystem,
                backgroundScope = backgroundScope,
                fileDispatcher = StandardTestDispatcher(testScheduler),
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
}

// The production bindings, with the file system and dispatchers the store takes as optional
// dependencies bound to test doubles.
@DependencyGraph(DeliveryScope::class, bindingContainers = [DeliveryBindings::class])
internal interface DeliveryTestGraph {
    val delivery: DeliveryPort

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides networkPort: NetworkPort,
            @Provides location: ContentCacheLocation,
            @Provides fileSystem: FileSystem,
            @Provides backgroundScope: CoroutineScope,
            @Provides fileDispatcher: CoroutineDispatcher,
        ): DeliveryTestGraph
    }
}

private object TestCacheLocation : ContentCacheLocation {
    override val root: Path = "/cache".toPath()
    override val legacyRoots: List<Path> = emptyList()
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
