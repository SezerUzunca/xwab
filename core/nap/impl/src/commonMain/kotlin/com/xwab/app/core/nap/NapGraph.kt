// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.nap

import com.xwab.app.core.nap.port.NAP_PLAYBACK_KIND
import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapPort
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
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

/** The lifetime of this module's own graph: one per application, owned by [NapGraphHolder]. */
internal object NapScope

/**
 * This module's own graph: the naps and the resolver are wired here. Only [NapPort] and the
 * resolver reach the application graph, through the two adapters below; speech is the one thing it
 * takes from outside.
 */
@DependencyGraph(NapScope::class)
internal interface NapGraph {
    val catalog: NapPort
    val resolver: PlaybackItemResolver

    @Binds val ManifestNapCatalogAdapter.bindCatalog: NapPort
    @Binds val NapPlaybackResolver.bindResolver: PlaybackItemResolver

    @Provides fun provideNaps(): List<Nap> = naps

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides speech: SpeechPort): NapGraph
    }
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class NapGraphHolder(speech: SpeechPort) {
    val graph: NapGraph = createGraphFactory<NapGraph.Factory>().create(speech)
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class NapCatalogGraphAdapter(holder: NapGraphHolder) : NapPort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(NAP_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class NapResolverGraphAdapter(holder: NapGraphHolder) : PlaybackItemResolver by holder.graph.resolver
