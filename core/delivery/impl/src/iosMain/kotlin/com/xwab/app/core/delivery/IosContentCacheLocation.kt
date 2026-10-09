@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.delivery

import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@Inject
internal class IosContentCacheLocation(
    fileManager: NSFileManager,
) : ContentCacheLocation {
    override val root: Path = (
        requireNotNull(
            fileManager.URLForDirectory(
                directory = NSCachesDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path,
        ) + "/content"
    ).toPath()
}
