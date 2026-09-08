package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest

/** Final files are visible only after a successful staged download. */
internal interface ContentFileStore {
    suspend fun find(key: CacheKey): String?
    suspend fun download(request: DeliveryRequest)
}

internal class UnusableContentSourceException(message: String) : Exception(message)
