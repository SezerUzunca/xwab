package com.xwab.app.core.shuffle

import android.content.Context
import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath

/** Reads `cacheDir` only when a track is rendered: the getter can create the directory, so it is disk work. */
@Inject
internal class AndroidShuffleCacheLocation(
    private val context: Context,
) : ShuffleCacheLocation {
    override fun root(): Path = context.cacheDir.resolve(SHUFFLE_DIRECTORY).absolutePath.toPath()

    private companion object {
        const val SHUFFLE_DIRECTORY = "shuffle"
    }
}
