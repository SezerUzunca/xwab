package com.xwab.app.core.delivery.port

/** Resolves caller-owned content to a cached file or HTTPS source and starts background caching. */
fun interface DeliveryPort {
    suspend fun resolve(request: DeliveryRequest): DeliveryResult
}

sealed interface DeliveryResult {
    data class Resolved(val uri: String) : DeliveryResult
    data class Unavailable(val reason: String?) : DeliveryResult
}
