package com.xwab.app

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

/** Called once, by the iOS app at launch. */
fun createAppRuntime(): AppRuntime = AppRuntime(createAppGraph())
