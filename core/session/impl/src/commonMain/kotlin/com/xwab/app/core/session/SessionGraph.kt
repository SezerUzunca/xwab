// The session is the one consumer of the resolver contract it owns.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/** The lifetime of this module's own graph: one per application, owned by [SessionGraphAdapter]. */
internal object SessionScope

/**
 * This module's own graph. Only [PlaybackPort] reaches the application graph; the engine and the
 * resolver providers content modules contributed there come in through the factory. Registering
 * a resolver provider does not construct its content graph just to start the session.
 */
@DependencyGraph(SessionScope::class)
internal interface SessionGraph {
    val playback: PlaybackPort

    @Binds val DefaultPlaybackAdapter.bindPlayback: PlaybackPort

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides enginePort: PlaybackEnginePort,
            @Provides resolvers: ContentResolvers,
        ): SessionGraph
    }
}

/**
 * The content modules' resolvers by playback kind, each built only when its provider is invoked.
 *
 * A named type rather than the bare map. Metro reads `Map<String, () -> PlaybackItemResolver>` as
 * a request for a multibinding's providers, and this graph has no multibinding: the map arrives as
 * a value from the application graph, where the multibinding is. Metro's guidance for a `() -> T`
 * carried as a value is a strongly typed wrapper
 * ([Metro intrinsics](https://github.com/ZacSweers/metro/blob/1.4.5/docs/metro-intrinsics.md)).
 */
internal class ContentResolvers(val byKind: Map<String, () -> PlaybackItemResolver>)

/**
 * Hands the module graph's port to the application graph, which builds it once. The resolver map
 * stays a provider multibinding of the application graph, so content modules plug in and out there
 * without all being constructed with the session; with none installed it is empty and the session
 * still exists.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class SessionGraphAdapter(
    enginePort: PlaybackEnginePort,
    resolverProvidersByKind: Map<String, () -> PlaybackItemResolver> = emptyMap(),
) : PlaybackPort by createGraphFactory<SessionGraph.Factory>()
    .create(enginePort, ContentResolvers(resolverProvidersByKind))
    .playback
