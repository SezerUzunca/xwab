package com.xwab.app.core.delivery

import dev.zacsweers.metro.createDynamicGraphFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Android production graph, with its cache location built by Metro from the cache directory. */
class AndroidDeliveryGraphTest {
    @Test
    fun prefetchUpdatesTheStoreAlreadyObservedThroughThePort() =
        prefetchUpdatesTheStoreAlreadyObservedThroughThePort { doubles, network ->
            createDynamicGraphFactory<AndroidDeliveryGraph.Factory>(doubles)
                .create(networkPort = network, cacheDirectory = File(CACHE_DIRECTORY))
        }

    @Test
    fun theCacheLivesInItsOwnDirectoryUnderTheAppCache() {
        val location = AndroidContentCacheLocation(File(CACHE_DIRECTORY))

        assertEquals(File(CACHE_DIRECTORY).resolve("content").absolutePath, location.root.toString())
    }

    private companion object {
        const val CACHE_DIRECTORY = "/cache"
    }
}
