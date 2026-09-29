package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Final files are visible only after a successful staged download. */
internal interface ContentFileStore {
    suspend fun find(key: CacheKey): String?
    suspend fun download(request: DeliveryRequest)
    fun observeCached(key: CacheKey): Flow<Boolean> = flowOf(false)
}

internal class UnusableContentSourceException(message: String) : Exception(message)
