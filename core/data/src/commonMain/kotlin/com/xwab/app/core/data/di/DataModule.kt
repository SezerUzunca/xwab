package com.xwab.app.core.data.di

import com.xwab.app.core.data.repository.BundledMusicCatalogRepository
import com.xwab.app.core.data.repository.DataStoreFavoritesRepository
import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import org.koin.dsl.module

/** Application-scoped bindings owned by the core:data module. */
val dataModule = module {
    single<MusicCatalogRepository> { BundledMusicCatalogRepository() }
    single<FavoritesRepository> { DataStoreFavoritesRepository(get()) }
}
