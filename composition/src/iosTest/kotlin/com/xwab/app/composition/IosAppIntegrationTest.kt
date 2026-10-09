package com.xwab.app.composition

import com.xwab.app.di.IosAppGraph
import dev.zacsweers.metro.createDynamicGraph
import kotlin.test.Test

/** The iOS production graph, with the offline data in place of the real catalogs and favourites. */
class IosAppIntegrationTest {
    @Test
    fun realEntriesOpenACategoryFromBrowse() = realEntriesOpenACategoryFromBrowse(::graph)

    @Test
    fun realEntriesOpenEveryScreen() = realEntriesOpenEveryScreen(::graph)

    private fun graph(data: OfflineCatalog): IosAppGraph = createDynamicGraph<IosAppGraph>(data)
}
