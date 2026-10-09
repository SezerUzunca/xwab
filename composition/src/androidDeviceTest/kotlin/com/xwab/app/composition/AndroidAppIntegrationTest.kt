package com.xwab.app.composition

import androidx.test.core.app.ApplicationProvider
import com.xwab.app.di.AndroidAppGraph
import dev.zacsweers.metro.createDynamicGraphFactory
import kotlin.test.Test

/** The Android production graph, with the offline data in place of the real catalogs and favourites. */
class AndroidAppIntegrationTest {
    @Test
    fun realEntriesOpenACategoryFromBrowse() = realEntriesOpenACategoryFromBrowse(::graph)

    @Test
    fun realEntriesOpenEveryScreen() = realEntriesOpenEveryScreen(::graph)

    private fun graph(data: OfflineCatalog): AndroidAppGraph =
        createDynamicGraphFactory<AndroidAppGraph.Factory>(data).create(ApplicationProvider.getApplicationContext())
}
