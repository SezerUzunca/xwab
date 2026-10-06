package com.xwab.app.core.network

import dev.zacsweers.metro.createDynamicGraph
import dev.zacsweers.metro.createGraph
import io.ktor.client.engine.HttpClientEngine

internal actual fun productionNetworkGraph(): NetworkGraph = createGraph<IosNetworkGraph>()

internal actual fun networkGraphWith(engine: HttpClientEngine): NetworkGraph =
    createDynamicGraph<IosNetworkGraph>(EngineReplacement(engine))
