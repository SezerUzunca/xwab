package com.xwab.app.core.favorites.di

import com.xwab.app.core.favorites.DataStoreFavoritesRepository
import com.xwab.app.core.favorites.FavoritesRepository
import org.koin.dsl.module

/**
 * Binds what this module persists. The store it is written to comes from
 * [favoritesPlatformModule], which is where the platform file path lives.
 */
val favoritesModule = module {
    single<FavoritesRepository> { DataStoreFavoritesRepository(get()) }
}
