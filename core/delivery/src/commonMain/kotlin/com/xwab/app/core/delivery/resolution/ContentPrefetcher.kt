package com.xwab.app.core.delivery.resolution

import com.xwab.app.core.delivery.port.DeliveryRequest

internal interface ContentPrefetcher {
    suspend fun prefetch(request: DeliveryRequest)
    fun close()
}
