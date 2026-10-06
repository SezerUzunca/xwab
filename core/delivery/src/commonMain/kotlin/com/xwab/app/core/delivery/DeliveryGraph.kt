package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.cache.CachingContentFileStore
import com.xwab.app.core.delivery.cache.ContentFileStore
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.resolution.BackgroundContentPrefetcher
import com.xwab.app.core.delivery.resolution.ContentPrefetcher
import com.xwab.app.core.delivery.resolution.LocalFirstDeliveryAdapter
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object DeliveryScope

/** The cache store, the prefetcher and the adapter: the same on every platform. */
@BindingContainer
internal interface DeliveryBindings {
    @Binds val CachingContentFileStore.bindFileStore: ContentFileStore
    @Binds val BackgroundContentPrefetcher.bindPrefetcher: ContentPrefetcher
    @Binds val LocalFirstDeliveryAdapter.bindDelivery: DeliveryPort
}

/**
 * What every platform's delivery graph hands out. Each platform declares its graph next to its
 * cache location, which is what differs between them; [DeliveryBindings] wires the rest, and only
 * [DeliveryPort] reaches the application graph.
 */
internal interface DeliveryGraph {
    val delivery: DeliveryPort
}
