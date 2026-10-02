package com.xwab.app.core.favorites

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath

@ContributesBinding(AppScope::class)
@Inject
internal class AndroidFavoritesFile(
    private val context: Context,
) : FavoritesFile {
    override fun path(): Path = context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
}
