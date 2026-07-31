package com.xwab.app.core.preferences.di

import com.xwab.app.core.preferences.DataStoreFavoritesRepository
import com.xwab.app.core.preferences.FavoritesRepository
import org.koin.dsl.module

/**
 * Binds the preferences this app persists. The store they are written to comes from
 * [preferencesPlatformModule], which is where the platform file path lives.
 */
val preferencesModule = module {
    single<FavoritesRepository> { DataStoreFavoritesRepository(get()) }
}
