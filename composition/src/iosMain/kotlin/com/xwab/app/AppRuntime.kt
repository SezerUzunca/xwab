package com.xwab.app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.xwab.app.di.AppGraph
import com.xwab.app.di.createAppGraph

/**
 * The application graph as the iOS app holds it: created once by the Swift `App`, which owns its
 * lifetime as `MainApplication` does on Android, and handed to each view controller it makes.
 *
 * Opaque to Swift. The graph itself stays inside the framework, so its header carries one type
 * rather than every binding the graph exposes to Kotlin.
 */
class AppRuntime internal constructor(internal val graph: AppGraph)

/**
 * Called once, by the iOS app at launch, with whether its build configuration is a debug one.
 *
 * That is the one logging decision the app makes: how much a debug and a release build write.
 * Kermit's global logger already writes to the platform log, and every module tags its own logger;
 * the modules receive that logger from their graphs, so it is set before the graph is built.
 */
fun createAppRuntime(isDebug: Boolean): AppRuntime {
    Logger.setMinSeverity(if (isDebug) Severity.Debug else Severity.Warn)
    return AppRuntime(createAppGraph())
}
