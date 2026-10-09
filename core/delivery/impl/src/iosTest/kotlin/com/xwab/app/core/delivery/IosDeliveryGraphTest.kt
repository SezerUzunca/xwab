package com.xwab.app.core.delivery

import dev.zacsweers.metro.createDynamicGraphFactory
import kotlin.test.Test
import kotlin.test.assertTrue
import platform.Foundation.NSFileManager

/** The iOS production graph, with its cache location built by Metro from the system's file manager. */
class IosDeliveryGraphTest {
    @Test
    fun prefetchUpdatesTheStoreAlreadyObservedThroughThePort() =
        prefetchUpdatesTheStoreAlreadyObservedThroughThePort { doubles, network ->
            createDynamicGraphFactory<IosDeliveryGraph.Factory>(doubles).create(networkPort = network)
        }

    @Test
    fun theCacheLivesInItsOwnDirectoryUnderTheCachesDirectory() {
        val root = IosContentCacheLocation(NSFileManager.defaultManager).root.toString()

        assertTrue(root.endsWith("Caches/content"), root)
    }
}
