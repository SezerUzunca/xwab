package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import com.xwab.app.core.network.port.NetworkTransportException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class KtorNetworkAdapter : NetworkPort {
    private val client: HttpClient

    @Inject
    constructor() {
        client = createNetworkHttpClient()
    }

    internal constructor(
        client: HttpClient,
    ) {
        this.client = client
    }

    override suspend fun download(
        httpsUrl: String,
        headers: Map<String, String>,
        onResponse: (NetworkResponse) -> Unit,
        onChunk: (bytes: ByteArray, count: Int) -> Unit,
    ) {
        requireHttps(httpsUrl)
        networkOperation {
            client.prepareGet(httpsUrl) {
                headers.forEach { (name, value) -> header(name, value) }
            }.execute { response ->
                requireHttps(response.call.request.url.toString())
                val metadata = NetworkResponse(
                    statusCode = response.status.value,
                    contentType = response.contentType()?.toString(),
                    contentLength = response.contentLength(),
                )
                downloadCallback { onResponse(metadata) }

                val channel = response.bodyAsChannel()
                val buffer = ByteArray(STREAM_BUFFER_BYTES)
                while (true) {
                    val count = channel.readAvailable(buffer)
                    if (count < 0) break
                    if (count > 0) downloadCallback { onChunk(buffer, count) }
                }
            }
        }
    }
}

private suspend inline fun <T> networkOperation(block: () -> T): T = try {
    block()
} catch (callback: DownloadCallbackFailure) {
    throw callback.original
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Exception) {
    // Ktor may unwrap a canceled request's cause. An inactive caller still owns cancellation.
    currentCoroutineContext().ensureActive()
    throw NetworkTransportException(failure)
}

/** Keep callback failures separate even when Ktor unwraps cancellation causes around execute. */
private inline fun downloadCallback(block: () -> Unit) {
    try {
        block()
    } catch (failure: Throwable) {
        throw DownloadCallbackFailure(failure)
    }
}

private class DownloadCallbackFailure(val original: Throwable) : RuntimeException()

/**
 * One client for every caller, with the two timeouts that mean the same thing to all of them:
 * a connection has to be established, and a transfer in progress has to keep progressing.
 *
 * There is deliberately **no request timeout here**. It would apply to the whole call including the
 * body, so the same number would have to serve a catalog document and a 25 MB download — and 25 MB
 * inside two minutes needs a sustained 1.75 Mbit/s, which is exactly what a listener on a weak
 * connection does not have.
 */
private fun createNetworkHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
    followRedirects = true
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

private fun requireHttps(rawUrl: String) {
    val isHttps = try {
        Url(rawUrl).protocol == URLProtocol.HTTPS
    } catch (_: Exception) {
        false
    }
    require(isHttps) { "Only valid HTTPS network requests are allowed." }
}

private const val STREAM_BUFFER_BYTES = 16 * 1024
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

/** Between two pieces of a transfer. A connection that stops sending fails; a slow one does not. */
private const val SOCKET_TIMEOUT_MILLIS = 30_000L
