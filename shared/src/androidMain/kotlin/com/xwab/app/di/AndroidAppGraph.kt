package com.xwab.app.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

/**
 * The application graph on Android, and the one runtime value it cannot derive: the [Context] the
 * cache directory, the DataStore file and the platform player are all built from.
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidAppGraph
    }
}

/** Built once, by the application object. */
fun createAppGraph(context: Context): AppGraph =
    createGraphFactory<AndroidAppGraph.Factory>().create(context)
