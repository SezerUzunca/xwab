package com.xwab.app

import android.app.Application
import com.xwab.app.di.AppGraph
import com.xwab.app.di.createAppGraph

/**
 * Builds the application graph once for the process.
 *
 * There is no global container to start any more: [appGraph] is the whole of the app's wiring, and
 * whoever needs something asks it rather than a service locator.
 */
class MainApplication : Application() {

    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createAppGraph(this)
    }
}
