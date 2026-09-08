package com.xwab.app.core.delivery.resolution

import co.touchlab.kermit.Logger
import com.xwab.app.core.delivery.cache.ContentFileStore
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.delivery.port.DeliveryResult
import kotlin.coroutines.cancellation.CancellationException

internal class LocalFirstDeliveryAdapter(
    private val fileStore: ContentFileStore,
    private val prefetcher: ContentPrefetcher,
) : DeliveryPort {
    private val logger = Logger.withTag("DeliveryPort")

    override suspend fun resolve(request: DeliveryRequest): DeliveryResult = try {
        val cachedPath = fileStore.find(request.key)
        if (cachedPath != null) {
            DeliveryResult.Resolved(cachedPath)
        } else {
            prefetcher.prefetch(request)
            DeliveryResult.Resolved(request.httpsUrl)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        logger.e(error) { "Could not resolve content for ${request.key}." }
        DeliveryResult.Unavailable(error.message)
    }
}
