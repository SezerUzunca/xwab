package com.xwab.app.core.delivery.port

import kotlinx.coroutines.flow.Flow

/** Resolves caller-owned content to a cached file or HTTPS source and starts background caching. */
interface DeliveryPort {
    /**
     * The URI to play [request] from: the absolute path of the cached file, or the request's own
     * HTTPS URL when there is none yet — in which case a background download starts. Content always
     * resolves; a cache that cannot be read falls back to HTTPS.
     */
    suspend fun resolve(request: DeliveryRequest): String

    /**
     * Whether a complete local file is present. Observation never starts a download.
     * Rechecks on collection and after cache changes; unreadable or unknown files report false.
     */
    fun observeCached(key: CacheKey): Flow<Boolean>
}
