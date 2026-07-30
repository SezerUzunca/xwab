package com.xwab.app.core.data.di

import com.xwab.app.core.data.AudioContentResolver
import com.xwab.app.core.data.DataStoreFavoritesRepository
import com.xwab.app.core.data.FavoritesRepository
import com.xwab.app.core.data.HybridAudioContentRepository
import com.xwab.app.core.data.MusicCatalogRepository
import org.koin.dsl.module
import org.koin.dsl.onClose

val dataModule = module {
    single { HybridAudioContentRepository(get()) } onClose { it?.close() }
    single<MusicCatalogRepository> { get<HybridAudioContentRepository>() }
    single<AudioContentResolver> { get<HybridAudioContentRepository>() }
    single<FavoritesRepository> { DataStoreFavoritesRepository(get()) }
}
