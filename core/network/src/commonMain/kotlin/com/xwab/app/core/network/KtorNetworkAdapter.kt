package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import com.xwab.app.core.network.port.NetworkTransportException
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
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

@SingleIn(NetworkScope::class)
@Inject
internal class KtorNetworkAdapter(
    private val client: HttpClient,
) : NetworkPort {

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

private fun requireHttps(rawUrl: String) {
    val isHttps = try {
        Url(rawUrl).protocol == URLProtocol.HTTPS
    } catch (_: Exception) {
        false
    }
    require(isHttps) { "Only valid HTTPS network requests are allowed." }
}

private const val STREAM_BUFFER_BYTES = 16 * 1024
