package com.xwab.app.core.delivery

import dev.zacsweers.metro.Inject
import java.io.File
import okio.Path
import okio.Path.Companion.toPath

@Inject
internal class AndroidContentCacheLocation(
    cacheDirectory: File,
) : ContentCacheLocation {
    override val root: Path = cacheDirectory.resolve("content").absolutePath.toPath()
}
