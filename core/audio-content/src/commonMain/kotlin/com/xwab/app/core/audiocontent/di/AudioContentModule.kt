package com.xwab.app.core.audiocontent.di

import com.xwab.app.core.audiocontent.AudioContentResolver
import com.xwab.app.core.audiocontent.DefaultMusicCatalogRepository
import com.xwab.app.core.audiocontent.HybridAudioContentResolver
import com.xwab.app.core.audiocontent.MusicCatalogRepository
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Binds both halves of the module: the catalog screens read, and the resolver playback asks for a
 * playable URI.
 *
 * The resolver's concrete type is registered as well as its port because it owns a background
 * download scope that has to be cancelled with the container — `onClose` needs the type that
 * declares `close()`.
 */
val audioContentModule = module {
    single<MusicCatalogRepository> { DefaultMusicCatalogRepository() }
    single { HybridAudioContentResolver(get()) } onClose { it?.close() }
    single<AudioContentResolver> { get<HybridAudioContentResolver>() }
}
