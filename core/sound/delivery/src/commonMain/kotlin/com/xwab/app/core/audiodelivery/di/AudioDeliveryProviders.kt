package com.xwab.app.core.audiodelivery.di

import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.audiodelivery.cache.CachingAudioFileStore
import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.audiodelivery.resolution.AudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.BackgroundAudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.LocalFirstAudioContentResolver
import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.network.NetworkClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.FileSystem
import okio.Path

/**
 * The one platform value Okio cannot choose: this app's sandboxed audio-cache directory.
 *
 * Public because a compile-time graph binds it by type; the Koin version could keep it internal
 * only because bindings were resolved by reflection at runtime.
 */
class AudioCacheRoot(val path: Path)

/**
 * Binds delivery: the resolver playback asks for a playable URI, and the prefetcher that fills the
 * cache behind it. Both read the manifest through `AudioSourceCatalog`, which is contributed by
 * `core:sound:manifest`. The cache directory comes from the platform half of this module.
 *
 * The prefetcher's background scope is no longer cancelled on container teardown: there is no
 * container, and the scope lives exactly as long as the process does.
 */
@ContributesTo(AppScope::class)
interface AudioDeliveryProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioFileStore(
        root: AudioCacheRoot,
        network: NetworkClient,
        sourceCatalog: AudioSourceCatalog,
    ): AudioFileStore =
        CachingAudioFileStore(
            fileSystem = FileSystem.SYSTEM,
            root = root.path,
            network = network,
            sourceCatalog = sourceCatalog,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioPrefetcher(store: AudioFileStore): AudioPrefetcher = BackgroundAudioPrefetcher(store)

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioContentResolver(
        sourceCatalog: AudioSourceCatalog,
        store: AudioFileStore,
        prefetcher: AudioPrefetcher,
    ): AudioContentResolver = LocalFirstAudioContentResolver(sourceCatalog, store, prefetcher)
}
