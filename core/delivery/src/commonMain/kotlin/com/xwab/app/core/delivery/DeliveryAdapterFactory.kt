package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.cache.CachingContentFileStore
import com.xwab.app.core.delivery.resolution.BackgroundContentPrefetcher
import com.xwab.app.core.delivery.resolution.LocalFirstDeliveryAdapter
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.delivery.port.DeliveryPort
import okio.FileSystem
import okio.Path

/**
 * Builds the hidden cache stack behind the one public delivery port.
 *
 * [legacyRoots] names cache directories this module used to write to and no longer reads. The
 * platform half knows those paths; the store drops them once, on its first download.
 */
internal fun createDeliveryAdapter(
    root: Path,
    networkPort: NetworkPort,
    legacyRoots: List<Path> = emptyList(),
): DeliveryPort {
    val fileStore = CachingContentFileStore(
        fileSystem = FileSystem.SYSTEM,
        root = root,
        networkPort = networkPort,
        legacyRoots = legacyRoots,
    )
    return LocalFirstDeliveryAdapter(
        fileStore = fileStore,
        prefetcher = BackgroundContentPrefetcher(fileStore),
    )
}
