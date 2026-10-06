package com.xwab.app.core.delivery

import android.content.Context
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
import java.io.File

/**
 * This module's own graph on Android. The application's cache directory comes in through the
 * factory — the one thing the graph needs from the platform, and narrower than a [Context], so a
 * host test can build the graph too.
 */
@DependencyGraph(DeliveryScope::class, bindingContainers = [DeliveryBindings::class])
internal interface AndroidDeliveryGraph : DeliveryGraph {
    @Binds val AndroidContentCacheLocation.bindLocation: ContentCacheLocation

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides networkPort: NetworkPort,
            @Provides cacheDirectory: File,
        ): AndroidDeliveryGraph
    }
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidDeliveryGraphAdapter(
    networkPort: NetworkPort,
    context: Context,
) : DeliveryPort by createGraphFactory<AndroidDeliveryGraph.Factory>()
    .create(networkPort, context.cacheDir)
    .delivery
