package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

/** The lifetime of this module's own graph: one per application, owned by [NetworkGraphAdapter]. */
internal object NetworkScope

/**
 * This module's own graph, which keeps Ktor inside the module: only [NetworkPort] reaches the
 * application graph. A binding container contributed to `AppScope` would have to be public to
 * reach that graph, and core declarations outside `.port` stay internal.
 */
@DependencyGraph(NetworkScope::class)
internal interface NetworkGraph {
    val network: NetworkPort

    @Binds val KtorNetworkAdapter.bindNetwork: NetworkPort

    /**
     * One client for every caller, with the two timeouts that mean the same thing to all of them:
     * a connection has to be established, and a transfer in progress has to keep progressing.
     *
     * There is deliberately **no request timeout here**. It would apply to the whole call including
     * the body, so the same number would have to serve a catalog document and a 25 MB download —
     * and 25 MB inside two minutes needs a sustained 1.75 Mbit/s, which is exactly what a listener
     * on a weak connection does not have.
     */
    @Provides
    @SingleIn(NetworkScope::class)
    fun provideHttpClient(): HttpClient = HttpClient {
        expectSuccess = false
        followRedirects = true
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        }
    }
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class NetworkGraphAdapter : NetworkPort by createGraph<NetworkGraph>().network

private const val CONNECT_TIMEOUT_MILLIS = 10_000L

/** Between two pieces of a transfer. A connection that stops sending fails; a slow one does not. */
private const val SOCKET_TIMEOUT_MILLIS = 30_000L
