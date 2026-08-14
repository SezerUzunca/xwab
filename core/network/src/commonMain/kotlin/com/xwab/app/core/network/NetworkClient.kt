package com.xwab.app.core.network

/** Metadata available before a streamed response body is consumed. */
data class NetworkResponse(
    val statusCode: Int,
    val contentType: String?,
    val contentLength: Long?,
)

/** A non-success response returned by an operation that expects a complete text document. */
class NetworkHttpException(val statusCode: Int) :
    IllegalStateException("Network request failed with HTTP $statusCode.")

/**
 * A request that did not finish inside the time allowed for it.
 *
 * A failure, deliberately, and not a `CancellationException`: a request nobody stopped is a server
 * or a network problem, and callers that treat cancellation as "the reader walked away" must not
 * mistake this for one.
 */
class NetworkTimeoutException(val timeoutMillis: Long) :
    IllegalStateException("Network request did not complete within $timeoutMillis ms.")

/**
 * The HTTP surface shared by content modules.
 *
 * Ktor stays an implementation detail of `core:network`: a catalog needs text and a media cache
 * needs chunks, not an engine, request builder or response pipeline.
 */
interface NetworkClient {
    /**
     * Fetches a small UTF-8 document.
     *
     * Non-2xx responses fail with [NetworkHttpException], and a document that does not arrive
     * within a short ceiling fails with [NetworkTimeoutException] — a catalog that has not answered
     * in seconds is not going to.
     *
     * **Nothing in the app calls this today.** Both catalogs ship with the build, so the only live
     * caller is [download]. It is kept, with its own tests, because it is the half of this port a
     * content feed needs, and because deleting a tested capability to add it back unchanged is
     * churn. If a feed is ruled out, this goes with it.
     */
    suspend fun getText(httpsUrl: String, headers: Map<String, String> = emptyMap()): String

    /**
     * Streams a response without retaining its body in memory.
     *
     * [onResponse] runs once before [onChunk], including for non-2xx responses, so the caller can
     * apply its own retry policy.
     *
     * Unlike [getText] this has **no ceiling on the whole transfer**: a 25 MB file on a slow
     * connection is not a failure, it is slow. What is enforced is progress — a connection that
     * stops sending fails on the socket timeout. A total limit here would cancel healthy downloads
     * on exactly the connections that need them most.
     *
     * The byte array passed to [onChunk] is **reused between chunks**: read it, write it, and let
     * it go. Keeping the reference hands you the *next* chunk's bytes, not the ones you were given.
     * It is reused rather than copied because a track is thousands of chunks and both callers write
     * each one straight to disk.
     */
    suspend fun download(
        httpsUrl: String,
        headers: Map<String, String> = emptyMap(),
        onResponse: (NetworkResponse) -> Unit,
        onChunk: (bytes: ByteArray, count: Int) -> Unit,
    )
}
