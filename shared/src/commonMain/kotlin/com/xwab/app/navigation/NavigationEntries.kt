package com.xwab.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * The entries `NavDisplay` should render for the tab that is showing, and the start tab beneath it.
 *
 * This is the whole of the multiple-back-stacks recipe from the Navigation 3 documentation, and it
 * lives beside [NavigationState] because it reads nothing else: [NavigationState.backStacks] and
 * [NavigationState.routesInUse] are the only inputs. Keeping it here is what lets the app shell
 * hold one line instead of the loop — the same split Now in Android makes, where `NiaApp` calls
 * `navigationState.toEntries(entryProvider)` and never sees a decorator.
 *
 * **Every** tab's stack is decorated, not only the selected one. Handing `NavDisplay` a list built
 * from the current tab alone would drop the other tabs' entries out of the display, and the
 * decorators clear the `ViewModelStore` and the saved state of every entry that leaves — so a tab
 * would come back scrolled to the top with its ViewModels rebuilt. `key` gives each iteration a
 * stable slot; without it every remembered stack would share one position in the composition.
 *
 * @param entryProvider resolves a route to the entry that draws it. Build it once and remember it:
 *   a new lambda on each recomposition defeats [rememberDecoratedNavEntries]' own caching.
 */
@Composable
internal fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val entriesByTab = backStacks.mapValues { (route, backStack) ->
        key(route) {
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
    }

    // `routesInUse` only ever answers with routes that have a stack, so a missing key is a broken
    // invariant rather than an empty tab. `NavigationState.currentBackStack` reads it the same way.
    return routesInUse.flatMap { entriesByTab.getValue(it) }
}
