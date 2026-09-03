package com.xwab.app.core.favorites.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwab.app.core.favorites.DATA_STORE_FILE_NAME
import com.xwab.app.core.favorites.createDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath

/** Where this device keeps the marked sounds. */
@ContributesTo(AppScope::class)
interface FavoritesAndroidProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideFavoritesDataStore(context: Context): DataStore<Preferences> =
        createDataStore(
            producePath = { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath() },
        )
}
