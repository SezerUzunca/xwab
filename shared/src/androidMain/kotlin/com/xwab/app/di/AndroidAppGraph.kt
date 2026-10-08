package com.xwab.app.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphPrivate
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders

/**
 * The application graph on Android, and the one runtime value it cannot derive: the [Context] the
 * cache directory, the DataStore file and the platform player are all built from.
 *
 * It also provides the app's Android components: MetroX's `AppComponentFactory` asks it for each
 * Activity contributed with `@ActivityKey`, so the launcher Activity is constructor-injected.
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph, MetroAppComponentProviders {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides @GraphPrivate context: Context): AndroidAppGraph
    }
}

/** Built once, by the application object. */
fun createAppGraph(context: Context): AndroidAppGraph =
    createGraphFactory<AndroidAppGraph.Factory>().create(context)
