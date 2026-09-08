package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort

internal fun requireUsableStatus(status: Int) {
    if (status in 400..499) throw UnusableContentSourceException("Content source answered HTTP $status.")
    check(status in 200..299) { "Content download failed with HTTP $status." }
}

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
