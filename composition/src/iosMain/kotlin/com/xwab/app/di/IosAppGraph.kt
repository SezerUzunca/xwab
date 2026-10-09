package com.xwab.app.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

/**
 * The application graph on iOS. Nothing is passed in: every platform value here — the caches
 * directory, the documents directory, the player — is read from `NSFileManager` and AVFoundation
 * by the module that needs it.
 */
@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph

/** Built once, by the view controller the app starts from. */
fun createAppGraph(): AppGraph = createGraph<IosAppGraph>()
