package com.xwab.app.core.network.port

/** Metadata available before a streamed response body is consumed. */
data class NetworkResponse(
    val statusCode: Int,
    val contentType: String?,
    val contentLength: Long?,
)

/**
 * A connection or response-stream failure, including transport-level timeouts.
 *
 * [cause] is diagnostic information; callers handle this port-owned type rather than inspecting
 * engine-specific exception types.
 */
class NetworkTransportException(cause: Throwable) :
    IllegalStateException("Network connection or response transfer failed.", cause)

/**
 * The HTTP port shared by core capabilities.
 *
 * Callers exchange only application-owned values through this boundary. Ktor, its engines and its
 * response pipeline remain private implementation details of `core:network`.
 *
 * Transport failures surface as [NetworkTransportException]. Caller cancellation is propagated
 * unchanged. Initial URLs rejected by the URL parser or using a non-HTTPS scheme fail with
 * [IllegalArgumentException].
 */
interface NetworkPort {
    /**
     * Streams a response without retaining its body in memory.
     *
     * [onResponse] runs once before [onChunk], including for non-success responses. The byte array
     * passed to [onChunk] is reused between chunks and must not be retained.
     * Exceptions thrown by either callback are propagated unchanged, not classified as network
     * failures: the callback owns response policy and destination writes.
     */
    suspend fun download(
        httpsUrl: String,
        headers: Map<String, String> = emptyMap(),
        onResponse: (NetworkResponse) -> Unit,
        onChunk: (bytes: ByteArray, count: Int) -> Unit,
    )
}
