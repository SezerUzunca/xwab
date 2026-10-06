package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.cache.CachingContentFileStore
import com.xwab.app.core.delivery.cache.ContentFileStore
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.resolution.BackgroundContentPrefetcher
import com.xwab.app.core.delivery.resolution.ContentPrefetcher
import com.xwab.app.core.delivery.resolution.LocalFirstDeliveryAdapter
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object DeliveryScope

/**
 * The cache store, the prefetcher and the adapter, and the resources they run on: the same on every
 * platform. A test replaces the resources through a dynamic graph.
 */
@BindingContainer
internal interface DeliveryBindings {
    @Binds val CachingContentFileStore.bindFileStore: ContentFileStore
    @Binds val BackgroundContentPrefetcher.bindPrefetcher: ContentPrefetcher
    @Binds val LocalFirstDeliveryAdapter.bindDelivery: DeliveryPort

    companion object {
        @Provides
        fun provideFileSystem(): FileSystem = FileSystem.SYSTEM

        /** The cache store's file work. The only dispatcher this graph binds. */
        @Provides
        fun provideFileDispatcher(): CoroutineDispatcher = Dispatchers.IO

        /** Where background downloads run; it lives as long as the graph, so the application. */
        @Provides
        @SingleIn(DeliveryScope::class)
        fun provideBackgroundScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** What the prefetcher measures its failure cooldown with. */
        @Provides
        fun provideTimeSource(): TimeSource = TimeSource.Monotonic
    }
}

/**
 * What every platform's delivery graph hands out. Each platform declares its graph next to its
 * cache location, which is what differs between them; [DeliveryBindings] wires the rest, and only
 * [DeliveryPort] reaches the application graph.
 */
internal interface DeliveryGraph {
    val delivery: DeliveryPort
}
