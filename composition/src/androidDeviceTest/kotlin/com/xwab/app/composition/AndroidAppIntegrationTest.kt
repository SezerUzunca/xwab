package com.xwab.app.composition

import androidx.test.core.app.ApplicationProvider
import com.xwab.app.di.AndroidAppGraph
import dev.zacsweers.metro.createDynamicGraphFactory
import kotlin.test.Test

/** The Android production graph, with the downloaded catalog replaced. */
class AndroidAppIntegrationTest {
    @Test
    fun realEntriesOpenACategoryFromBrowse() = realEntriesOpenACategoryFromBrowse { catalog ->
        createDynamicGraphFactory<AndroidAppGraph.Factory>(catalog).create(ApplicationProvider.getApplicationContext())
    }
}
