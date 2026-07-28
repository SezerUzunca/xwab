package com.xwab.app.core.favorites.di

import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.favorites.DataStoreFavoritesRepository
import org.koin.dsl.module

val favoritesModule = module {
    single<FavoritesRepository> { DataStoreFavoritesRepository(get()) }
}
