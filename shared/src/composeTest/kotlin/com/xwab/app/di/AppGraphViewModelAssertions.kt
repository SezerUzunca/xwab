package com.xwab.app.di

import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * What a platform's real graph must hold for screens to open, checked where it is generated.
 *
 * `checkArchitecture` already proves every ViewModel is annotated. This proves the annotations
 * reach the graph on the platform at hand, and that each assisted factory resolves to the type it
 * is keyed by — the lookup `assistedMetroViewModel` makes. Resolving a factory builds no ports;
 * resolving a ViewModel would, so plain ViewModels are only checked to be present.
 */
internal fun assertEveryViewModelResolves(graph: AppGraph) {
    assertSame(graph.metroViewModelFactory, graph.metroViewModelFactory)
    assertTrue(graph.viewModelProviders.isNotEmpty(), "No ViewModel reached the app graph.")
    assertTrue(
        graph.manualAssistedFactoryProviders.isNotEmpty(),
        "No assisted ViewModel factory reached the app graph.",
    )
    graph.manualAssistedFactoryProviders.forEach { (key, provider) ->
        val factory = provider()
        assertTrue(key.isInstance(factory), "$key resolved to ${factory::class}.")
    }
}
