package com.xwab.app.composition

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.navigation.NavigationState
import dev.zacsweers.metro.createGraphFactory

/** The entry graph for one host, over the stacks that host restored. */
internal fun appEntryGraph(state: NavigationState): AppEntryGraph =
    createGraphFactory<AppEntryGraph.Factory>().create(state)

/**
 * Installs the feature entries [graph] collected, wired to that graph's navigator.
 *
 * Features receive their own callback contracts. Their ViewModels resolve from the factory in
 * composition only when an entry is drawn, so collecting installers creates no ViewModel or port.
 */
internal fun appEntryProvider(graph: AppEntryGraph): (NavKey) -> NavEntry<NavKey> = entryProvider {
    graph.entryProviderInstallers.forEach { install -> install() }
}
