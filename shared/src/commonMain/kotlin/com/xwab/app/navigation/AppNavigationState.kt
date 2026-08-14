package com.xwab.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration

/**
 * The navigation facade the shell renders from: which tabs exist, which one is selected, and the
 * entries `NavDisplay` should show for it. [com.xwab.app.App] renders from this facade without
 * owning the navigation setup.
 */
internal class AppNavigationState(
    val destinations: List<TopLevelDestination>,
    private val navigationState: NavigationState,
    private val navigator: Navigator,
) {
    val selectedRoute: NavKey get() = navigationState.topLevelRoute

    fun navigate(route: NavKey) = navigator.navigate(route)

    fun goBack() = navigator.goBack()

    @Composable
    fun entries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> =
        navigationState.toEntries(entryProvider)
}

/**
 * Builds and restores [AppNavigationState]: one back stack per tab and the selected tab. Feature
 * entry wiring remains in the application composition root.
 */
@Composable
internal fun rememberAppNavigationState(): AppNavigationState {
    val destinations = TOP_LEVEL_DESTINATIONS
    val configuration = remember { SavedStateConfiguration { serializersModule = FEATURE_SERIALIZERS } }

    // One per tab, each rooted at its own destination. `key` gives each loop iteration a stable
    // Compose slot instead of having every remembered stack share the same position.
    val backStacks: Map<NavKey, NavBackStack<NavKey>> = destinations.associate { destination ->
        destination.route to key(destination.route) {
            rememberNavBackStack(configuration, destination.route)
        }
    }

    // The single source of truth for the selected tab: handed straight to `NavigationState`
    // rather than mirrored into it through a callback, so there is only ever one place it lives.
    val selectedRoute = rememberSelectedRoute(destinations)
    val navigationState = remember {
        NavigationState(
            startRoute = destinations.first().route,
            backStacks = backStacks,
            topLevelRouteState = selectedRoute,
        )
    }
    val navigator = remember(navigationState) { Navigator(navigationState) }

    return remember(navigationState, navigator) {
        AppNavigationState(destinations, navigationState, navigator)
    }
}

/** Persists the selected tab as an index so Kotlin Multiplatform needs no NavKey saver. */
@Composable
private fun rememberSelectedRoute(destinations: List<TopLevelDestination>): MutableState<NavKey> =
    rememberSaveable(
        saver = Saver(
            save = { selected ->
                destinations.indexOfFirst { it.route == selected.value }.coerceAtLeast(0)
            },
            restore = { index ->
                mutableStateOf(destinations.getOrNull(index)?.route ?: destinations.first().route)
            },
        ),
    ) {
        mutableStateOf(destinations.first().route)
    }
