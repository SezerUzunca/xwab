package com.xwab.app.di

import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * The application graph's shared surface: the ViewModel factory the app root places in
 * composition.
 *
 * The graph itself is declared per platform — the Android one takes a `Context`, the iOS one takes
 * nothing — and both implement this. Metro merges each adapter's generated contribution provider
 * into them, and each feature's ViewModels into the [ViewModelGraph] maps, so no binding is named
 * here: a new capability or screen contributes its own, and what this module adds is the
 * dependency on the module that holds it. Metro aggregates a scope's contributions from the compile
 * classpath, so a module missing from `shared/build.gradle.kts` never reaches the graph.
 *
 * The maps hold providers. A ViewModel, and the ports behind it, are created only when an entry
 * asks for one; the now-playing bar asks on the first frame, so the playback session starts with
 * the app.
 */
interface AppGraph : ViewModelGraph
