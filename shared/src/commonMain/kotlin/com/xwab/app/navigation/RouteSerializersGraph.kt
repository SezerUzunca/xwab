package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metro.DependencyGraph
import kotlinx.serialization.modules.SerializersModule

/**
 * The route serializers features contribute with `@ContributesTo(NavKey::class)`.
 *
 * A graph of its own rather than part of `AppEntryGraph`: the back stacks are restored with these
 * before the navigator, and so the callbacks entries need, exists. `NavKey` is the scope because
 * features and shared both already see it.
 */
@DependencyGraph(NavKey::class)
internal interface RouteSerializersGraph {
    val routeSerializers: Set<SerializersModule>
}
