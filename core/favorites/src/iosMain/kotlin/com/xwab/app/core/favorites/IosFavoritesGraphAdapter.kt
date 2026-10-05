package com.xwab.app.core.favorites

import com.xwab.app.core.favorites.port.FavoritesPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/**
 * Hands the module graph's port to the application graph, which builds it once. The favorites file
 * is described here, so it never becomes a binding of the application graph.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosFavoritesGraphAdapter : FavoritesPort by createGraphFactory<FavoritesGraph.Factory>()
    .create(IosFavoritesFile())
    .favorites
