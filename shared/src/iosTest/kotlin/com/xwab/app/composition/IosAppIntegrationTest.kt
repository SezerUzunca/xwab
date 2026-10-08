package com.xwab.app.composition

import com.xwab.app.di.IosAppGraph
import dev.zacsweers.metro.createDynamicGraph
import kotlin.test.Test

/** The iOS production graph, with the downloaded catalog replaced. */
class IosAppIntegrationTest {
    @Test
    fun realEntriesOpenACategoryFromBrowse() = realEntriesOpenACategoryFromBrowse { catalog ->
        createDynamicGraph<IosAppGraph>(catalog)
    }
}
