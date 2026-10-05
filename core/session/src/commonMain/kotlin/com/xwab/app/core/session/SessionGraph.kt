// The session is the one consumer of the resolver contract it owns.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/** The lifetime of this module's own graph: one per application, owned by [SessionGraphAdapter]. */
internal object SessionScope

@BindingContainer
internal interface SessionBindings {
    @Binds val DefaultPlaybackAdapter.bindPlayback: PlaybackPort
}

/**
 * This module's own graph. Only [PlaybackPort] reaches the application graph; the engine and the
 * resolvers content modules contributed there come in through the factory.
 */
@DependencyGraph(SessionScope::class, bindingContainers = [SessionBindings::class])
internal interface SessionGraph {
    val playback: PlaybackPort

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides enginePort: PlaybackEnginePort,
            @Provides resolversByKind: Map<String, PlaybackItemResolver>,
        ): SessionGraph
    }
}

/**
 * Hands the module graph's port to the application graph, which builds it once. The resolver map
 * stays a multibinding of the application graph, so content modules plug in and out there; with
 * none installed it is empty and the session still exists.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class SessionGraphAdapter(
    enginePort: PlaybackEnginePort,
    resolversByKind: Map<String, PlaybackItemResolver> = emptyMap(),
) : PlaybackPort by createGraphFactory<SessionGraph.Factory>().create(enginePort, resolversByKind).playback
