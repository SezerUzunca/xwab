package com.xwab.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import com.xwab.app.navigation.NavigationState

/**
 * The entries `NavDisplay` should render: the tab that is showing, and the start tab beneath it.
 *
 * Named `remember` rather than a conversion, because it is one. Each tab's decorators are held
 * across recompositions and the call is what keeps them alive — the returned list is this frame's
 * view of state this function owns, not a value derived from [state] on the spot.
 *
 * Each tab retains its own decorators, following the Navigation 3 multiple-back-stacks recipe.
 * Every tab's decorator stays in composition, inactive ones included, so its saved state and
 * ViewModel stores survive a switch away and back. `key(route)` ties each tab's remembered state to
 * that tab rather than to its position in the map.
 *
 * Tab-scoped content keys come from [entryProviderForTab], which is what keeps the same feature
 * route independent in two tabs once their entries are combined in one `NavDisplay`.
 *
 * @param state the back stack per tab, and which tab is showing.
 * @param entryProvider resolves a route to the entry that draws it. Navigation 3 caches these
 *   entries by back-stack contents; changing only the provider does not invalidate them.
 * @param onUp closes the given destination's pane; the back arrow's decorator calls it.
 * @param resultEventBus the host graph's bus, shared with the navigator that sends tab reselections.
 */
@Composable
internal fun rememberTabEntries(
    state: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    metadataProvider: (NavKey, NavKey, List<NavKey>) -> Map<String, Any>,
    onUp: (NavKey) -> Unit,
    resultEventBus: ResultEventBus,
): List<NavEntry<NavKey>> {
    val entriesByTab = state.backStacks.mapValues { (route, backStack) ->
        key(route) {
            val tabEntryProvider = remember(route, backStack, entryProvider, metadataProvider) {
                entryProviderForTab(route, backStack, entryProvider, metadataProvider)
            }
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    // In this order: the saveable decorator gives each entry its own saved-state
                    // registry and the store decorator hands it to the entry's ViewModels, so a
                    // SavedStateHandle is saved and restored with its entry rather than the root.
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(resultEventBus),
                    rememberAdaptiveBackNavEntryDecorator(onUp),
                ),
                entryProvider = tabEntryProvider,
            )
        }
    }

    // `routesInUse` only ever answers with routes that have a stack, so a missing key is a broken
    // invariant rather than an empty tab. `NavigationState.currentBackStack` reads it the same way.
    return state.routesInUse.flatMap { entriesByTab.getValue(it) }
}
