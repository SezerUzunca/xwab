package com.xwab.app.core.delivery.port

/** Resolves caller-owned content to a cached file or HTTPS source and starts background caching. */
interface DeliveryPort {
    suspend fun resolve(request: DeliveryRequest): DeliveryResult

    /**
     * Drops cached content for namespaces this build no longer installs.
     *
     * Ordinary cache sweeping happens inside a delivery request, against that request's own
     * namespace. That is exactly what a removed content type stops producing: no request is ever
     * made for its namespace again, so its directory is never listed and its files stay until the
     * platform reclaims the cache on its own — which for audio means tens of megabytes sitting
     * behind a content type the app no longer has.
     *
     * Only the composition root knows which namespaces are installed, so it is the caller.
     *
     * @param namespaces every namespace still owned by a content module in this build. An empty
     *   set would delete the whole cache, so it is refused rather than obeyed: a caller that
     *   computed nothing is far more likely to be broken than to mean it.
     */
    suspend fun retainOnly(namespaces: Set<String>)
}

sealed interface DeliveryResult {
    data class Resolved(val uri: String) : DeliveryResult
    data class Unavailable(val reason: String?) : DeliveryResult
}
