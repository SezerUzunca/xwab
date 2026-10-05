package com.xwab.app.core.delivery

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

internal class AndroidContentCacheLocation(
    context: Context,
) : ContentCacheLocation {
    override val root: Path = context.cacheDir.resolve("content").absolutePath.toPath()

    /** Where sound-only delivery cached its tracks, before any of this was namespaced. */
    override val legacyRoots: List<Path> =
        listOf(context.cacheDir.resolve("audio-content").absolutePath.toPath())
}
