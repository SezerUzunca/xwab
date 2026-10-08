package com.xwab.app

import android.app.Application
import com.xwab.app.di.createAppGraph
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication

/**
 * Builds the application graph once for the process.
 *
 * There is no global container to start any more: the graph is the whole of the app's wiring.
 * MetroX's `AppComponentFactory`, merged in from its manifest, reads [appComponentProviders] to
 * construct each Activity, so nothing asks this object for the graph by hand.
 */
class MainApplication : Application(), MetroApplication {
    override val appComponentProviders: MetroAppComponentProviders by lazy { createAppGraph(this) }
}
