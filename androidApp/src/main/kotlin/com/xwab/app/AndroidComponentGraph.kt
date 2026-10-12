package com.xwab.app

import com.xwab.app.di.AppGraph
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders

/** The scope [AndroidComponentGraph] collects the Android components of this module from. */
object AndroidComponentScope

/**
 * The Android components the platform asks this app for, through MetroX's `AppComponentFactory`:
 * the launcher Activity, contributed with `@ActivityKey`.
 *
 * A graph of its own, in the app module, because a graph collects only what its module's compile
 * classpath holds. The Activity lives here, above the composition root, so the application graph
 * there cannot see it; this graph includes that one instead, and builds the Activity from its
 * accessors — the ViewModel factory. Its own scope keeps it from collecting the `AppScope`
 * contributions the composition root's public classes carry.
 */
@DependencyGraph(AndroidComponentScope::class)
interface AndroidComponentGraph : MetroAppComponentProviders {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Includes appGraph: AppGraph): AndroidComponentGraph
    }
}

/** Built once, by the application object, over the application graph. */
internal fun createComponentGraph(appGraph: AppGraph): AndroidComponentGraph =
    createGraphFactory<AndroidComponentGraph.Factory>().create(appGraph)
