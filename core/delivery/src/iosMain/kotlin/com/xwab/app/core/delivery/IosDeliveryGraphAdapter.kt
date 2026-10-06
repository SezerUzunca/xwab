package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.network.port.NetworkPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import platform.Foundation.NSFileManager

/** This module's own graph on iOS. It needs nothing from the platform beyond the system's file manager. */
@DependencyGraph(DeliveryScope::class, bindingContainers = [DeliveryBindings::class])
internal interface IosDeliveryGraph : DeliveryGraph {
    @Binds val IosContentCacheLocation.bindLocation: ContentCacheLocation

    @Provides
    fun provideFileManager(): NSFileManager = NSFileManager.defaultManager

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides networkPort: NetworkPort): IosDeliveryGraph
    }
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosDeliveryGraphAdapter(
    networkPort: NetworkPort,
) : DeliveryPort by createGraphFactory<IosDeliveryGraph.Factory>()
    .create(networkPort)
    .delivery
