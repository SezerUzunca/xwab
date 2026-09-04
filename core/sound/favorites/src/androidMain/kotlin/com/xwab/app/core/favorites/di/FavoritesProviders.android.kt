package com.xwab.app.core.favorites.di

import android.content.Context
import com.xwab.app.core.favorites.DATA_STORE_FILE_NAME
import com.xwab.app.core.favorites.DataStoreFavoritesRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.favorites.createDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath

/**
 * What this device remembers, and where it keeps it.
 *
 * The store is built here rather than bound, so DataStore stops at this module: what the graph
 * carries is [FavoritesRepository], which is the only thing a screen ever asked for.
 */
@ContributesTo(AppScope::class)
interface FavoritesAndroidProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideFavoritesRepository(context: Context): FavoritesRepository =
        DataStoreFavoritesRepository(
            createDataStore(
                producePath = { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath() },
            ),
        )
}
