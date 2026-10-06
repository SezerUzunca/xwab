package com.xwab.app.core.favorites

import android.content.Context
import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath

/** Reads `filesDir` only when DataStore asks: the getter can create the directory, so it is disk work. */
@Inject
internal class AndroidFavoritesFile(
    private val context: Context,
) : FavoritesFile {
    override fun path(): Path = context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
}
