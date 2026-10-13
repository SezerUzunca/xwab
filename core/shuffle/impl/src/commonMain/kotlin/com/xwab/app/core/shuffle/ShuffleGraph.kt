// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShufflePort
import com.xwab.app.core.speech.port.SpeechPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey
import dev.zacsweers.metro.createGraphFactory
import kotlin.random.Random

/** The lifetime of this module's own graph: one per application, owned by [ShuffleGraphHolder]. */
internal object ShuffleScope

/**
 * This module's own graph: the decks, their words and the resolver are wired here. Only
 * [ShufflePort] and the resolver reach the application graph, through the two adapters below;
 * speech is the one thing it takes from outside.
 */
@DependencyGraph(ShuffleScope::class)
internal interface ShuffleGraph {
    val catalog: ShufflePort
    val resolver: PlaybackItemResolver

    @Binds val ManifestShuffleCatalogAdapter.bindCatalog: ShufflePort
    @Binds val ShufflePlaybackResolver.bindResolver: PlaybackItemResolver

    @Provides fun provideDecks(): List<ShuffleDeck> = shuffleDecks
    @Provides fun provideWordLists(): List<ShuffleWordList> = shuffleWordLists

    /** Which words a track draws and in what order; a test hands a seeded one. */
    @Provides fun provideRandom(): Random = Random.Default

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides speech: SpeechPort): ShuffleGraph
    }
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class ShuffleGraphHolder(speech: SpeechPort) {
    val graph: ShuffleGraph = createGraphFactory<ShuffleGraph.Factory>().create(speech)
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class ShuffleCatalogGraphAdapter(holder: ShuffleGraphHolder) : ShufflePort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(SHUFFLE_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class ShuffleResolverGraphAdapter(
    holder: ShuffleGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
