@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.delivery

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@Inject
internal class IosContentCacheLocation : ContentCacheLocation {
    override val root: Path = iosCachePath("content")

    /** Where sound-only delivery cached its tracks, before any of this was namespaced. */
    override val legacyRoots: List<Path> = listOf(iosCachePath("audio-content"))
}

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
