package com.xwab.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * The entries `NavDisplay` should render for the tab that is showing, and the start tab beneath it.
 *
 * Each tab retains its own decorators, following the Navigation 3 multiple-back-stacks recipe.
 * Content keys include the tab identity so the same feature route in two tabs remains independent
 * when their entries are combined in one NavDisplay.
 *
 * Every tab's decorator remains in composition, including inactive tabs, to retain its saved
 * state and ViewModel stores. `key(route)` keeps each tab's remembered state tied to that tab.
 *
 * @param entryProvider resolves a route to the entry that draws it. Navigation 3 1.1.1 caches
 *   these entries by back-stack contents; changing only the provider does not invalidate them.
 */
@Composable
internal fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val entriesByTab = backStacks.mapValues { (route, backStack) ->
        key(route) {
            val tabEntryProvider = remember(route, entryProvider) {
                entryProviderForTab(route, entryProvider)
            }
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = tabEntryProvider,
            )
        }
    }

    // `routesInUse` only ever answers with routes that have a stack, so a missing key is a broken
    // invariant rather than an empty tab. `NavigationState.currentBackStack` reads it the same way.
    return routesInUse.flatMap { entriesByTab.getValue(it) }
}
