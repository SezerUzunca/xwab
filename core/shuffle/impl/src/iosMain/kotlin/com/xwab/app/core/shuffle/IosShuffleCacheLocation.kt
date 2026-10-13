@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.shuffle

import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The app's caches directory, looked up when a track is rendered rather than when the graph is built. */
@Inject
internal class IosShuffleCacheLocation(
    private val fileManager: NSFileManager,
) : ShuffleCacheLocation {
    override fun root(): Path = (
        requireNotNull(
            fileManager.URLForDirectory(
                directory = NSCachesDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path,
        ) + "/$SHUFFLE_DIRECTORY"
    ).toPath()

    private companion object {
        const val SHUFFLE_DIRECTORY = "shuffle"
    }
}
