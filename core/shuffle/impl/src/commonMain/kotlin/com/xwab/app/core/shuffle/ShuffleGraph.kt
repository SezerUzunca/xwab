// The graph hands out the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import co.touchlab.kermit.Logger
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShufflePort
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
import okio.FileSystem

/** The lifetime of this module's own graph: one per application, owned by the platform's graph holder. */
internal object ShuffleScope

/**
 * The catalog, the renderer and the resolver, and what they run on: the same on every platform.
 * Only the voice and the cache location differ, and each platform's graph binds its own.
 */
@BindingContainer
internal interface ShuffleBindings {
    @Binds val ManifestShuffleCatalogAdapter.bindCatalog: ShufflePort
    @Binds val ShufflePlaybackResolver.bindResolver: PlaybackItemResolver

    companion object {
        @Provides fun provideDecks(): List<ShuffleDeck> = shuffleDecks
        @Provides fun provideWordLists(): List<ShuffleWordList> = shuffleWordLists

        @Provides fun provideFileSystem(): FileSystem = FileSystem.SYSTEM

        /** The renderer's file and voice work, off the thread that asked for the track. */
        @Provides fun provideFileDispatcher(): CoroutineDispatcher = Dispatchers.IO

        /** Which words a track draws and in what order; a test hands a seeded one. */
        @Provides fun provideRandom(): Random = Random.Default

        /**
         * Kermit's logger, whose level the application sets before this graph is built. Each class
         * tags its own; a test hands its class one that records what it writes.
         */
        @Provides fun provideLogger(): Logger = Logger
    }
}

/**
 * What every platform's shuffle graph hands out. Only [ShufflePort] and the resolver reach the
 * application graph; the words, the voice and the files stay inside this module.
 */
internal interface ShuffleGraph {
    val catalog: ShufflePort
    val resolver: PlaybackItemResolver
}
