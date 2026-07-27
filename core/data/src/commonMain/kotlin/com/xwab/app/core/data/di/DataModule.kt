package com.xwab.app.core.data.di

import com.xwab.app.core.repository.DataStoreFavoritesRepository
import com.xwab.app.core.playback.DefaultPlaybackCoordinator
import com.xwab.app.core.playback.PlaybackCoordinator
import com.xwab.app.core.repository.FavoritesRepository
import com.xwab.app.core.repository.LocalMusicRepository
import com.xwab.app.core.repository.MusicRepository
import org.koin.dsl.module

/** Application-scoped bindings owned by the core:data module. */
val dataModule = module {
    single<MusicRepository> { LocalMusicRepository() }
    single<FavoritesRepository> { DataStoreFavoritesRepository(get()) }
    single<PlaybackCoordinator> { DefaultPlaybackCoordinator(get()) }
}
