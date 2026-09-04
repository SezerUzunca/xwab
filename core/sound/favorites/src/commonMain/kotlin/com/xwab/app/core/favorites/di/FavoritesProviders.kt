package com.xwab.app.core.favorites.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwab.app.core.favorites.DataStoreFavoritesRepository
import com.xwab.app.core.favorites.FavoritesRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Binds what this module persists. The store it is written to comes from the platform half of this
 * module, which is where the file path lives.
 */
@ContributesTo(AppScope::class)
interface FavoritesProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideFavoritesRepository(store: DataStore<Preferences>): FavoritesRepository =
        DataStoreFavoritesRepository(store)
}
