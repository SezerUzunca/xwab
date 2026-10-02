@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.favorites

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@Inject
internal class IosFavoritesFile : FavoritesFile {
    override fun path(): Path {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return (requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME").toPath()
    }
}
