package com.xwab.app.core.audiodelivery.di

import com.xwab.app.core.audiodelivery.cache.CachingAudioFileStore
import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
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
 * Public because a compile-time graph binds it by type, and the platform half of this module is
 * what provides it; the Koin version could keep it internal only because bindings were resolved by
 * reflection at runtime.
 */
class AudioCacheRoot(val path: Path)

/**
 * Binds delivery, and only what leaves the module: the resolver playback asks for a playable URI.
 *
 * The store and the prefetcher stay internal and are constructed here rather than bound. A
 * compile-time graph can only carry types its consumers can name, and nothing outside this module
 * has any business naming either — the session asks for a URI, not for a cache.
 *
 * The prefetcher's background scope is no longer cancelled on container teardown: there is no
 * container, and the scope lives exactly as long as the process does.
 */
@ContributesTo(AppScope::class)
interface AudioDeliveryProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioContentResolver(
        root: AudioCacheRoot,
        network: NetworkClient,
        sourceCatalog: AudioSourceCatalog,
    ): AudioContentResolver {
        val fileStore = CachingAudioFileStore(
            fileSystem = FileSystem.SYSTEM,
            root = root.path,
            network = network,
            sourceCatalog = sourceCatalog,
        )
        return LocalFirstAudioContentResolver(
            fileStore = fileStore,
            prefetcher = BackgroundAudioPrefetcher(fileStore),
            sourceCatalog = sourceCatalog,
        )
    }
}
