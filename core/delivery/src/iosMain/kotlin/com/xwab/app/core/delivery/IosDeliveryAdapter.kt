@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.delivery

import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.delivery.port.DeliveryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosDeliveryAdapter(
    networkPort: NetworkPort,
) : DeliveryPort by createDeliveryAdapter(
    root = iosCachePath("content"),
    networkPort = networkPort,
    // Where sound-only delivery cached its tracks, before any of this was namespaced.
    legacyRoots = listOf(iosCachePath("audio-content")),
)

private fun iosCachePath(directoryName: String) = (
    requireNotNull(
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path,
    ) + "/" + directoryName
).toPath()
