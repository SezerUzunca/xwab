package com.xwab.app.core.delivery.resolution

import co.touchlab.kermit.Logger
import com.xwab.app.core.delivery.DeliveryScope
import com.xwab.app.core.delivery.cache.ContentFileStore
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow

@SingleIn(DeliveryScope::class)
@Inject
internal class LocalFirstDeliveryAdapter(
    private val fileStore: ContentFileStore,
    private val prefetcher: ContentPrefetcher,
) : DeliveryPort {
    private val logger = Logger.withTag("DeliveryPort")

    override fun observeCached(key: CacheKey): Flow<Boolean> = fileStore.observeCached(key)

    override suspend fun resolve(request: DeliveryRequest): String = try {
        fileStore.find(request.key) ?: run {
            prefetcher.prefetch(request)
            request.httpsUrl
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        logger.w(error) { "Cache unavailable for ${request.key}; streaming from HTTPS." }
        request.httpsUrl
    }
}
