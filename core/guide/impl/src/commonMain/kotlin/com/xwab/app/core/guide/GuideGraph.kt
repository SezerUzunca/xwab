// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.guide

import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuidePort
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

/** The lifetime of this module's own graph: one per application, owned by [GuideGraphHolder]. */
internal object GuideScope

/**
 * This module's own graph: the guides, their scripts and the resolver are wired here. Only
 * [GuidePort] and the resolver reach the application graph, through the two adapters below; speech
 * is the one thing it takes from outside.
 */
@DependencyGraph(GuideScope::class)
internal interface GuideGraph {
    val catalog: GuidePort
    val resolver: PlaybackItemResolver

    @Binds val ManifestGuideCatalogAdapter.bindCatalog: GuidePort
    @Binds val GuidePlaybackResolver.bindResolver: PlaybackItemResolver

    @Provides fun provideGuides(): List<Guide> = guides
    @Provides fun provideScripts(): List<GuideScript> = guideScripts

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides speech: SpeechPort): GuideGraph
    }
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class GuideGraphHolder(speech: SpeechPort) {
    val graph: GuideGraph = createGraphFactory<GuideGraph.Factory>().create(speech)
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class GuideCatalogGraphAdapter(holder: GuideGraphHolder) : GuidePort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(GUIDE_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class GuideResolverGraphAdapter(
    holder: GuideGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
