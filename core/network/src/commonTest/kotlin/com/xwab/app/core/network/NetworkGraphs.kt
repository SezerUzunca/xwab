package com.xwab.app.core.network

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import io.ktor.client.engine.HttpClientEngine

/** This platform's production graph, unchanged. */
internal expect fun productionNetworkGraph(): NetworkGraph

/**
 * This platform's production graph with [engine] in place of the real one, so the client's own
 * configuration — timeouts, redirects, status handling — is what a test exercises.
 */
internal expect fun networkGraphWith(engine: HttpClientEngine): NetworkGraph

/** Replaces the platform engine through Metro's dynamic graph. */
@BindingContainer
internal class EngineReplacement(private val engine: HttpClientEngine) {
    @Provides
    fun engine(): HttpClientEngine = engine
}
