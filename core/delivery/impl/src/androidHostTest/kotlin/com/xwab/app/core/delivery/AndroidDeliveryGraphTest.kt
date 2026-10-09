package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import dev.zacsweers.metro.createDynamicGraphFactory
import dev.zacsweers.metro.createGraphFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** The Android production graph, with its cache location built by Metro from the cache directory. */
class AndroidDeliveryGraphTest {
    @Test
    fun prefetchUpdatesTheStoreAlreadyObservedThroughThePort() =
        prefetchUpdatesTheStoreAlreadyObservedThroughThePort { doubles, network ->
            createDynamicGraphFactory<AndroidDeliveryGraph.Factory>(doubles)
                .create(networkPort = network, cacheDirectory = File(CACHE_DIRECTORY))
        }

    /**
     * Nothing swapped: the graph's own defaults — the real file system, `Dispatchers.IO` and the
     * prefetcher's own scope — download into a real directory, and the next resolve finds the file.
     */
    @Test
    fun theUnmodifiedGraphCachesToDiskAndResolvesTheFileNextTime() = runBlocking {
        val cacheDirectory = Files.createTempDirectory("delivery-graph").toFile()
        try {
            val delivery = createGraphFactory<AndroidDeliveryGraph.Factory>()
                .create(networkPort = BodyNetwork, cacheDirectory = cacheDirectory)
                .delivery
            val request = DeliveryRequest(CacheKey("sound", "rain.mp3"), "https://example.test/rain.mp3")

            assertEquals(request.httpsUrl, delivery.resolve(request))
            withTimeout(TIMEOUT_MS) { delivery.observeCached(request.key).first { it } }

            val cached = cacheDirectory.resolve("content/sound/rain.mp3")
            assertContentEquals(BODY, cached.readBytes())
            assertEquals(cached.absolutePath, File(delivery.resolve(request)).absolutePath)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun theCacheLivesInItsOwnDirectoryUnderTheAppCache() {
        val location = AndroidContentCacheLocation(File(CACHE_DIRECTORY))

        assertEquals(File(CACHE_DIRECTORY).resolve("content").absolutePath, location.root.toString())
    }

    private object BodyNetwork : NetworkPort {
        override suspend fun download(
            httpsUrl: String,
            headers: Map<String, String>,
            onResponse: (NetworkResponse) -> Unit,
            onChunk: (bytes: ByteArray, count: Int) -> Unit,
        ) {
            onResponse(NetworkResponse(HTTP_OK, "audio/mpeg", BODY.size.toLong()))
            onChunk(BODY, BODY.size)
        }
    }

    private companion object {
        const val CACHE_DIRECTORY = "/cache"
        const val TIMEOUT_MS = 5_000L
        const val HTTP_OK = 200
        val BODY = byteArrayOf(1, 2, 3)
    }
}
