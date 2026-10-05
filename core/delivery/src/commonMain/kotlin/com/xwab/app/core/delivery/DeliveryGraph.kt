package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.cache.CachingContentFileStore
import com.xwab.app.core.delivery.cache.ContentFileStore
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.resolution.BackgroundContentPrefetcher
import com.xwab.app.core.delivery.resolution.ContentPrefetcher
import com.xwab.app.core.delivery.resolution.LocalFirstDeliveryAdapter
import com.xwab.app.core.network.port.NetworkPort
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object DeliveryScope

@BindingContainer
internal interface DeliveryBindings {
    @Binds val CachingContentFileStore.bindFileStore: ContentFileStore
    @Binds val BackgroundContentPrefetcher.bindPrefetcher: ContentPrefetcher
    @Binds val LocalFirstDeliveryAdapter.bindDelivery: DeliveryPort
}

/**
 * This module's own graph: the cache store, the prefetcher and the adapter are wired here, and
 * only [DeliveryPort] reaches the application graph. What it needs from outside — the network and
 * the platform's cache location — arrives through the factory.
 */
@DependencyGraph(DeliveryScope::class, bindingContainers = [DeliveryBindings::class])
internal interface DeliveryGraph {
    val delivery: DeliveryPort

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides networkPort: NetworkPort,
            @Provides location: ContentCacheLocation,
        ): DeliveryGraph
    }
}
