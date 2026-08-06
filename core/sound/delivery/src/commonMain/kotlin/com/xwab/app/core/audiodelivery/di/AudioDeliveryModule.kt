package com.xwab.app.core.audiodelivery.di

import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.audiodelivery.resolution.AudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.BackgroundAudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.LocalFirstAudioContentResolver
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Binds delivery: the resolver playback asks for a playable URI, and the prefetcher that fills the
 * cache behind it. Both read the manifest through `core:sound:catalog`'s `AudioSourceCatalog`, which is
 * bound by that module.
 *
 * The prefetcher is the only binding with a lifecycle — it owns the background download scope that
 * has to be cancelled with the container — so it is the only one that needs `onClose`.
 */
val audioDeliveryModule = module {
    single<AudioPrefetcher> { BackgroundAudioPrefetcher(get()) } onClose { it?.close() }
    single<AudioContentResolver> { LocalFirstAudioContentResolver(get(), get(), get()) }
}
