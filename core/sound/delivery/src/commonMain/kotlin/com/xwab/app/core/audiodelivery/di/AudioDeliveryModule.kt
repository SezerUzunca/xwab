package com.xwab.app.core.audiodelivery.di

import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.audiodelivery.cache.CachingAudioFileStore
import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.audiodelivery.resolution.AudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.BackgroundAudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.LocalFirstAudioContentResolver
import okio.FileSystem
import okio.Path
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/** The one platform value Okio cannot choose: this app's sandboxed audio-cache directory. */
internal class AudioCacheRoot(val path: Path)

expect val audioDeliveryPlatformModule: Module

/**
 * Binds delivery: the resolver playback asks for a playable URI, and the prefetcher that fills the
 * cache behind it. Both read the manifest through `AudioSourceCatalog`, which is declared and bound
 * by `core:sound:manifest`.
 *
 * The prefetcher is the only binding with a lifecycle — it owns the background download scope that
 * has to be cancelled with the container — so it is the only one that needs `onClose`.
 */
val audioDeliveryModule = module {
    single<AudioFileStore> {
        CachingAudioFileStore(
            fileSystem = FileSystem.SYSTEM,
            root = get<AudioCacheRoot>().path,
            network = get(),
            sourceCatalog = get(),
        )
    }
    single<AudioPrefetcher> { BackgroundAudioPrefetcher(get()) } onClose { it?.close() }
    single<AudioContentResolver> { LocalFirstAudioContentResolver(get(), get(), get()) }
}
