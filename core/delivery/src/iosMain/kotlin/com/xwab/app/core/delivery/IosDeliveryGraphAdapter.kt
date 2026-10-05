package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.network.port.NetworkPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/**
 * Hands the module graph's port to the application graph, which builds it once. The cache location
 * is built here, so it never becomes a binding of the application graph.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosDeliveryGraphAdapter(
    networkPort: NetworkPort,
) : DeliveryPort by createGraphFactory<DeliveryGraph.Factory>()
    .create(networkPort, IosContentCacheLocation())
    .delivery
