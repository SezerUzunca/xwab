// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

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
import dev.zacsweers.metro.createGraph
import platform.Foundation.NSFileManager

/** This module's own graph on iOS. It needs nothing from the platform beyond the system's file manager. */
@DependencyGraph(ShuffleScope::class, bindingContainers = [ShuffleBindings::class])
internal interface IosShuffleGraph : ShuffleGraph {
    @Binds val IosWordVoice.bindVoice: WordVoice
    @Binds val IosShuffleCacheLocation.bindLocation: ShuffleCacheLocation

    @Provides
    fun provideFileManager(): NSFileManager = NSFileManager.defaultManager
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class IosShuffleGraphHolder {
    val graph: ShuffleGraph = createGraph<IosShuffleGraph>()
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosShuffleCatalogGraphAdapter(
    holder: IosShuffleGraphHolder,
) : ShufflePort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(SHUFFLE_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class IosShuffleResolverGraphAdapter(
    holder: IosShuffleGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
