package com.xwab.app.core.network

import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class NetworkGraphTest {
    /** The production graph, on each platform: Metro builds the adapter around its one client. */
    @Test
    fun theModuleGraphProvidesOneClientBackedAdapter() {
        val graph = createGraph<NetworkGraph>()

        assertIs<KtorNetworkAdapter>(graph.network)
        assertSame(graph.network, graph.network)
    }
}
