package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkPort
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object NetworkScope

/**
 * The adapter and the client it runs on: the same on every platform. Only the engine differs, and
 * each platform's graph provides its own.
 */
@BindingContainer
internal interface NetworkBindings {
    @Binds val KtorNetworkAdapter.bindNetwork: NetworkPort

    companion object {
        /**
         * One client for every caller, with the two timeouts that mean the same thing to all of
         * them: a connection has to be established, and a transfer in progress has to keep
         * progressing.
         *
         * There is deliberately **no request timeout here**. It would apply to the whole call
         * including the body, so the same number would have to serve a catalog document and a
         * 25 MB download — and 25 MB inside two minutes needs a sustained 1.75 Mbit/s, which is
         * exactly what a listener on a weak connection does not have.
         */
        @Provides
        @SingleIn(NetworkScope::class)
        fun provideHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
            expectSuccess = false
            followRedirects = true
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
        }
    }
}

/**
 * What every platform's network graph hands out. Only [NetworkPort] reaches the application graph,
 * which keeps Ktor inside this module.
 */
internal interface NetworkGraph {
    val network: NetworkPort
}

private const val CONNECT_TIMEOUT_MILLIS = 10_000L

/** Between two pieces of a transfer. A connection that stops sending fails; a slow one does not. */
private const val SOCKET_TIMEOUT_MILLIS = 30_000L
