// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import android.content.Context
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.shuffle.port.ShufflePort
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

/**
 * This module's own graph on Android. It takes the [Context], which the voice needs to reach the
 * device's text-to-speech engine; the cache directory is read from it only when a track is made.
 */
@DependencyGraph(ShuffleScope::class, bindingContainers = [ShuffleBindings::class])
internal interface AndroidShuffleGraph : ShuffleGraph {
    @Binds val AndroidWordVoice.bindVoice: WordVoice
    @Binds val AndroidShuffleCacheLocation.bindLocation: ShuffleCacheLocation

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidShuffleGraph
    }
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class AndroidShuffleGraphHolder(context: Context) {
    val graph: ShuffleGraph = createGraphFactory<AndroidShuffleGraph.Factory>().create(context)
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidShuffleCatalogGraphAdapter(
    holder: AndroidShuffleGraphHolder,
) : ShufflePort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(SHUFFLE_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class AndroidShuffleResolverGraphAdapter(
    holder: AndroidShuffleGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
