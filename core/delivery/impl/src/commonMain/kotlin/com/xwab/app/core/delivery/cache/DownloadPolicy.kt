package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort

/**
 * A 4xx is the source refusing, and a 3xx that reaches this point is a redirect the client would
 * not follow — the network client follows every other one, but never from HTTPS to cleartext.
 * Neither changes on a retry, so both are unusable; a 5xx may pass, so it fails retryably.
 */
internal fun requireUsableStatus(status: Int) {
    if (status in FIRST_REDIRECT..LAST_CLIENT_ERROR) {
        throw UnusableContentSourceException("Content source answered HTTP $status.")
    }
    check(status in FIRST_SUCCESS..LAST_SUCCESS) { "Content download failed with HTTP $status." }
}

private const val FIRST_SUCCESS = 200
private const val LAST_SUCCESS = 299
private const val FIRST_REDIRECT = 300
private const val LAST_CLIENT_ERROR = 499

internal fun requireUsableContentType(rawContentType: String?, accepted: Set<String>) {
    if (accepted.isEmpty()) return
    val type = rawContentType.orEmpty().substringBefore(';').trim().lowercase()
    if (accepted.none { it.substringBefore(';').trim().lowercase() == type }) {
        throw UnusableContentSourceException("Unexpected content type: $type")
    }
}

internal fun requireWithinSizeLimit(bytes: Long, maxBytes: Long) {
    if (bytes > maxBytes) throw UnusableContentSourceException("Content download is too large: $bytes bytes.")
}

internal suspend fun NetworkPort.downloadContent(
    request: DeliveryRequest,
    writeChunk: (bytes: ByteArray, count: Int) -> Unit,
) {
    var declaredLength: Long? = null
    var received = 0L
    download(
        httpsUrl = request.httpsUrl,
        // The caller's headers first, then the derived `Accept`, which the request refuses to carry
        // itself — so what a source is offered can never disagree with what it is checked against.
        headers = request.headers +
            ("Accept" to request.acceptedContentTypes.sorted().joinToString(", ").ifEmpty { "*/*" }),
        onResponse = { response ->
            requireUsableStatus(response.statusCode)
            requireUsableContentType(response.contentType, request.acceptedContentTypes)
            declaredLength = response.contentLength?.takeIf { it >= 0 }
            declaredLength?.let { requireWithinSizeLimit(it, request.maxBytes) }
        },
        onChunk = { bytes, count ->
            received += count
            requireWithinSizeLimit(received, request.maxBytes)
            writeChunk(bytes, count)
        },
    )
    check(declaredLength == null || received == declaredLength) { "Downloaded content is incomplete." }
}
