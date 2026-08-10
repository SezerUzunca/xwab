package com.xwab.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.xwab.app.core.navigation.NavigationState
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.core.navigation.rememberNavigator
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.di.appEntryProvider
import com.xwab.app.di.featureSerializers
import com.xwab.app.di.topLevelDestinations

/**
 * The app's frame: a navigation bar built from whatever the features declare, and one back stack
 * per tab beneath it.
 *
 * This file names no feature and no route. A slice appears on screen by publishing a
 * `TopLevelDestination` on its `FeatureEntry` and being added to `features` — so the sleep slices
 * that come next cost one line each here, and none at all in the features that already exist.
 *
 * The structure follows the multiple-back-stacks recipe in the Navigation 3 documentation:
 * `rememberDecoratedNavEntries` per stack, flattened into the one list `NavDisplay` renders. That
 * is not a style choice. Handing `NavDisplay` a different back stack on every tab switch would drop
 * the other tab's entries out of the display, and the decorators clear a `ViewModelStore` and the
 * saved state of every entry that leaves — so a tab would come back scrolled to the top with its
 * ViewModels rebuilt. Decorating each stack separately keeps the ones that are not showing alive.
 */
@Composable
fun AppShell() {
    val destinations = remember { topLevelDestinations }
    check(destinations.isNotEmpty()) { "No feature declares a TopLevelDestination." }
    val startRoute = destinations.first().route

    // Collected from the feature list; this file names no route of its own, not even the start one.
    val configuration = remember { SavedStateConfiguration { serializersModule = featureSerializers } }

    // One per tab, each rooted at its own destination. `key` because these are remembered inside a
    // loop, where every iteration otherwise shares one position in the composition.
    val backStacks: Map<NavKey, NavBackStack<NavKey>> = destinations.associate { destination ->
        destination.route to key(destination.route) {
            rememberNavBackStack(configuration, destination.route)
        }
    }

    val selectedRoute = rememberSelectedRoute(destinations)
    val state = remember(destinations) {
        NavigationState(
            startRoute = startRoute,
            backStacks = backStacks,
            topLevelRoute = selectedRoute,
        )
    }
    val navigator = rememberNavigator(state)

    // Each feature contributes its own `entry<Route> { }` blocks and is handed the navigator to
    // route with, so this file still names no route — only that features have entries at all.
    // Rebuilt only when the navigator identity changes, which is never after the first composition.
    val entryProvider = remember(navigator) { appEntryProvider(navigator) }
    val entriesByTab = backStacks.mapValues { (route, backStack) ->
        key(route) {
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                ),
                entryProvider = entryProvider,
            )
        }
    }
    val entries = state.routesInUse.flatMap { entriesByTab[it].orEmpty() }

    Scaffold(
        // The screens paint their own gradient; this is only what shows behind the bar.
        containerColor = SleepRelaxTheme.colors.backgroundBottom,
        bottomBar = {
            AppNavigationBar(
                destinations = destinations,
                selectedRoute = state.topLevelRoute,
                onSelect = navigator::navigate,
            )
        },
    ) { innerPadding ->
        NavDisplay(
            entries = entries,
            onBack = navigator::goBack,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun AppNavigationBar(
    destinations: List<TopLevelDestination>,
    selectedRoute: NavKey,
    onSelect: (NavKey) -> Unit,
) {
    NavigationBar(containerColor = SleepRelaxTheme.colors.backgroundBottom) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.route == selectedRoute,
                onClick = { onSelect(destination.route) },
                icon = destination.icon,
                label = { Text(destination.label()) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SleepRelaxTheme.colors.accent,
                    selectedTextColor = SleepRelaxTheme.colors.accent,
                    unselectedIconColor = SleepRelaxTheme.colors.textSecondary,
                    unselectedTextColor = SleepRelaxTheme.colors.textSecondary,
                    indicatorColor = SleepRelaxTheme.colors.glassWhite,
                ),
            )
        }
    }
}

/**
 * The selected tab, saved as its position rather than as a route.
 *
 * The documented recipe persists a `NavKey` directly, through a serializer that
 * `navigation3-runtime` 1.1.1 publishes for Android only. An index needs no serializer at all and
 * survives process death the same way; a list that has since changed shape restores to the start
 * tab instead of to whatever now sits at that position.
 */
@Composable
private fun rememberSelectedRoute(destinations: List<TopLevelDestination>): MutableState<NavKey> =
    rememberSaveable(
        destinations,
        saver = Saver<MutableState<NavKey>, Int>(
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
