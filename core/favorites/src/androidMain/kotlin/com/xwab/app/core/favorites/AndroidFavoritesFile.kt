package com.xwab.app.core.favorites

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

internal class AndroidFavoritesFile(
    private val context: Context,
) : FavoritesFile {
    override fun path(): Path = context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
}
