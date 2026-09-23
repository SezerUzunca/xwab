package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest

/** Final files are visible only after a successful staged download. */
internal interface ContentFileStore {
    suspend fun find(key: CacheKey): String?
    suspend fun download(request: DeliveryRequest)

    /** Removes whole namespace directories the app no longer installs. */
    suspend fun retainOnly(namespaces: Set<String>)
}

internal class UnusableContentSourceException(message: String) : Exception(message)
