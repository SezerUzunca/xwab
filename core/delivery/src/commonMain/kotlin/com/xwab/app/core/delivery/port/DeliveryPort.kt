package com.xwab.app.core.delivery.port

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Resolves caller-owned content to a cached file or HTTPS source and starts background caching. */
interface DeliveryPort {
    suspend fun resolve(request: DeliveryRequest): DeliveryResult

    /**
     * Whether a complete local file is present. Observation never starts a download.
     * Rechecks on collection and after cache changes; unreadable or unknown files report false.
     */
    fun observeCached(key: CacheKey): Flow<Boolean> = flowOf(false)
}

sealed interface DeliveryResult {
    data class Resolved(val uri: String) : DeliveryResult
    data class Unavailable(val reason: String?) : DeliveryResult
}
