package com.xwab.app.core.network.port

/** Metadata available before a streamed response body is consumed. */
public data class NetworkResponse(
    public val statusCode: Int,
    public val contentType: String?,
    public val contentLength: Long?,
)

/** A non-success response returned by an operation that expects a complete text document. */
public class NetworkHttpException(public val statusCode: Int) :
    IllegalStateException("Network request failed with HTTP $statusCode.")

/** A request that did not finish inside the time allowed for it. */
public class NetworkTimeoutException(public val timeoutMillis: Long) :
    IllegalStateException("Network request did not complete within $timeoutMillis ms.")

/**
 * The HTTP port shared by core capabilities.
 *
 * Callers exchange only application-owned values through this boundary. Ktor, its engines and its
 * response pipeline remain private implementation details of `core:network`.
 */
public interface NetworkPort {
    /** Fetches a small UTF-8 document. */
    public suspend fun getText(
        httpsUrl: String,
        headers: Map<String, String> = emptyMap(),
    ): String

    /**
     * Streams a response without retaining its body in memory.
     *
     * [onResponse] runs once before [onChunk], including for non-success responses. The byte array
     * passed to [onChunk] is reused between chunks and must not be retained.
     */
    public suspend fun download(
        httpsUrl: String,
        headers: Map<String, String> = emptyMap(),
        onResponse: (NetworkResponse) -> Unit,
        onChunk: (bytes: ByteArray, count: Int) -> Unit,
    )
}
