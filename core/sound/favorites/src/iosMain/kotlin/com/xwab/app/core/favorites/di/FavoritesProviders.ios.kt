@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.favorites.di

import com.xwab.app.core.favorites.DATA_STORE_FILE_NAME
import com.xwab.app.core.favorites.DataStoreFavoritesRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.favorites.createDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * What this device remembers, and where it keeps it.
 *
 * The store is built here rather than bound, so DataStore stops at this module: what the graph
 * carries is [FavoritesRepository], which is the only thing a screen ever asked for.
 */
@ContributesTo(AppScope::class)
interface FavoritesIosProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideFavoritesRepository(): FavoritesRepository =
        DataStoreFavoritesRepository(
            createDataStore(
                producePath = {
                    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                        directory = NSDocumentDirectory,
                        inDomain = NSUserDomainMask,
                        appropriateForURL = null,
                        create = false,
                        error = null,
                    )
                    (requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME").toPath()
                },
            ),
        )
}
