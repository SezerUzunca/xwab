package com.xwab.app.core.playback.platform

import com.xwab.app.core.playback.port.PlaybackEnginePort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph

/** This module's own graph on iOS. Only [PlaybackEnginePort] reaches the application graph. */
@DependencyGraph(PlaybackScope::class)
internal interface IosPlaybackGraph {
    val engine: PlaybackEnginePort

    @Binds val IosPlaybackFacade.bindEngine: PlaybackEnginePort
}

/**
 * Hands the module graph's engine to the application graph, which builds it once — on the main
 * thread, as the engine requires.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosPlaybackGraphAdapter : PlaybackEnginePort by createGraph<IosPlaybackGraph>().engine
