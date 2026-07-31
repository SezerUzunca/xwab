package com.xwab.app.core.audiocontent.di

import com.xwab.app.core.audiocontent.AudioContentResolver
import com.xwab.app.core.audiocontent.AudioPrefetcher
import com.xwab.app.core.audiocontent.BackgroundAudioPrefetcher
import com.xwab.app.core.audiocontent.DefaultMusicCatalogRepository
import com.xwab.app.core.audiocontent.LocalFirstAudioContentResolver
import com.xwab.app.core.audiocontent.MusicCatalogRepository
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Binds both halves of the module: the catalog screens read, and the resolver playback asks for a
 * playable URI.
 *
 * The prefetcher is the only binding with a lifecycle — it owns the background download scope that
 * has to be cancelled with the container — so it is the only one that needs `onClose`.
 */
val audioContentModule = module {
    single<MusicCatalogRepository> { DefaultMusicCatalogRepository() }
    single<AudioPrefetcher> { BackgroundAudioPrefetcher(get()) } onClose { it?.close() }
    single<AudioContentResolver> { LocalFirstAudioContentResolver(get(), get()) }
}
