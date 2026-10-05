package com.xwab.app.core.playback.platform

import android.content.Context
import com.xwab.app.core.playback.port.PlaybackEnginePort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/**
 * This module's own graph on Android. Only [PlaybackEnginePort] reaches the application graph; the
 * [Context] the engine connects to the playback service with comes in through the factory.
 */
@DependencyGraph(PlaybackScope::class)
internal interface AndroidPlaybackGraph {
    val engine: PlaybackEnginePort

    @Binds val AndroidPlaybackFacade.bindEngine: PlaybackEnginePort

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidPlaybackGraph
    }
}

/**
 * Hands the module graph's engine to the application graph, which builds it once — on the main
 * thread, as the engine requires.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidPlaybackGraphAdapter(
    context: Context,
) : PlaybackEnginePort by createGraphFactory<AndroidPlaybackGraph.Factory>().create(context).engine
