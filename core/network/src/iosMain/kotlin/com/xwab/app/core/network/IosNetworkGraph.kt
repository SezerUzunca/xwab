package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/** This module's own graph on iOS, where the client runs on Darwin's URLSession. */
@DependencyGraph(NetworkScope::class, bindingContainers = [NetworkBindings::class])
internal interface IosNetworkGraph : NetworkGraph {
    @Provides
    @SingleIn(NetworkScope::class)
    fun provideEngine(): HttpClientEngine = Darwin.create()
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosNetworkGraphAdapter : NetworkPort by createGraph<IosNetworkGraph>().network
