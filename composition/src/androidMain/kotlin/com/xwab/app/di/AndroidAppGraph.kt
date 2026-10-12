package com.xwab.app.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphPrivate
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

/**
 * The application graph on Android, and the one runtime value it cannot derive: the [Context] the
 * cache directory, the DataStore file and the platform player are all built from.
 *
 * The Android app provides that value and keeps the graph; the components Android constructs from
 * it, such as the launcher Activity, are that app's own graph's to build.
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides @GraphPrivate context: Context): AndroidAppGraph
    }
}

/** Built once, by the application object. */
fun createAppGraph(context: Context): AndroidAppGraph =
    createGraphFactory<AndroidAppGraph.Factory>().create(context)
