package com.xwab.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

internal class KtorNetworkClient(
    private val client: HttpClient,
    private val textTimeoutMillis: Long = TEXT_TIMEOUT_MILLIS,
) : NetworkClient {
    /**
     * The ceiling is applied here rather than on the client, because it must not apply to
     * [download]: one setting covering both would cancel a healthy 25 MB transfer at the moment a
     * catalog request is considered late.
     *
     * The timeout becomes a [NetworkTimeoutException] instead of propagating as a cancellation, so
     * a caller cannot mistake "the server never answered" for "my reader went away".
     */
    override suspend fun getText(httpsUrl: String, headers: Map<String, String>): String {
        requireHttps(httpsUrl)
        return withTimeoutOrNull(textTimeoutMillis.milliseconds) {
            client.prepareGet(httpsUrl) {
                headers.forEach { (name, value) -> header(name, value) }
            }.execute { response ->
                requireHttps(response.call.request.url.toString())
                if (response.status.value !in 200..299) {
                    throw NetworkHttpException(response.status.value)
                }
                response.bodyAsText()
            }
        } ?: throw NetworkTimeoutException(textTimeoutMillis)
    }

    override suspend fun download(
        httpsUrl: String,
        headers: Map<String, String>,
        onResponse: (NetworkResponse) -> Unit,
        onChunk: (bytes: ByteArray, count: Int) -> Unit,
    ) {
        requireHttps(httpsUrl)
        client.prepareGet(httpsUrl) {
            headers.forEach { (name, value) -> header(name, value) }
        }.execute { response ->
            requireHttps(response.call.request.url.toString())
            onResponse(
                NetworkResponse(
                    statusCode = response.status.value,
                    contentType = response.contentType()?.toString(),
                    contentLength = response.contentLength(),
                ),
            )

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            while (true) {
                val count = channel.readAvailable(buffer)
                if (count < 0) break
                if (count > 0) onChunk(buffer, count)
            }
        }
    }
}

/**
 * One client for every caller, with the two timeouts that mean the same thing to all of them:
 * a connection has to be established, and a transfer in progress has to keep progressing.
 *
 * There is deliberately **no request timeout here**. It would apply to the whole call including the
 * body, so the same number would have to serve a catalog document and a 25 MB download — and 25 MB
 * inside two minutes needs a sustained 1.75 Mbit/s, which is exactly what a listener on a weak
 * connection does not have. `getText` sets its own ceiling instead.
 */
internal fun createNetworkHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
    followRedirects = true
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

private fun requireHttps(rawUrl: String) {
    require(Url(rawUrl).protocol == URLProtocol.HTTPS) { "Only HTTPS network requests are allowed." }
}

private const val STREAM_BUFFER_BYTES = 16 * 1024
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

/** Between two pieces of a transfer. A connection that stops sending fails; a slow one does not. */
private const val SOCKET_TIMEOUT_MILLIS = 30_000L

/** A whole catalog document. It is small, so if it is late it is not coming. */
private const val TEXT_TIMEOUT_MILLIS = 15_000L
