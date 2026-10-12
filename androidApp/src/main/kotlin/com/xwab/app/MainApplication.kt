package com.xwab.app

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.xwab.app.di.createAppGraph
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication

/**
 * Builds the application graph once for the process, from the one value only Android has: this
 * [android.content.Context].
 *
 * There is no global container to start any more: the graph is the whole of the app's wiring.
 * MetroX's `AppComponentFactory`, merged in from its manifest, reads [appComponentProviders] to
 * construct each Activity, so nothing asks this object for the graph by hand.
 */
class MainApplication : Application(), MetroApplication {
    init {
        // The one logging decision this app makes: how much a debug and a release build write.
        // Kermit's global logger already writes to Logcat, and every module tags its own logger.
        // Here rather than in onCreate: MetroX builds the graphs while it installs the content
        // providers, which Android does first, and the modules' loggers come from those graphs.
        Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Debug else Severity.Warn)
    }

    override val appComponentProviders: MetroAppComponentProviders by lazy {
        createComponentGraph(createAppGraph(this))
    }
}
