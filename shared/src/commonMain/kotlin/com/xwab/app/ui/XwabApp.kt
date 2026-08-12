package com.xwab.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.xwab.app.core.navigation.NavigationState
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.core.navigation.toEntries
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.navigation.FEATURE_SERIALIZERS
import com.xwab.app.navigation.TOP_LEVEL_DESTINATIONS
import com.xwab.app.navigation.TopLevelDestination
import com.xwab.app.navigation.appEntryProvider

/**
 * The application-level Compose entry point, analogous to Now in Android's `NiaApp`.
 *
 * It owns the scaffold and the app's navigation state while remaining unaware of individual screens
 * and routes. The theme is applied by [com.xwab.app.App], not here, so this stays renderable under
 * a different one.
 *
 * There is no separate AppState yet because navigation is the only app-wide state; introduce one
 * when the shell gains another independent source — an offline flag or a snackbar, which is the
 * point at which Now in Android splits `NiaApp` into a stateful outer and a parameterised inner.
 */
@Composable
internal fun XwabApp() {
    val destinations = TOP_LEVEL_DESTINATIONS
    val configuration = remember { SavedStateConfiguration { serializersModule = FEATURE_SERIALIZERS } }

    // One per tab, each rooted at its own destination. `key` gives each loop iteration a stable
    // Compose slot instead of having every remembered stack share the same position.
    val backStacks: Map<NavKey, NavBackStack<NavKey>> = destinations.associate { destination ->
        destination.route to key(destination.route) {
            rememberNavBackStack(configuration, destination.route)
        }
    }

    val selectedRoute = rememberSelectedRoute(destinations)
    val state = remember {
        NavigationState(
            startRoute = destinations.first().route,
            backStacks = backStacks,
            initialTopLevelRoute = selectedRoute.value,
            onTopLevelRouteChanged = { selectedRoute.value = it },
        )
    }
    val navigator = remember(state) { Navigator(state) }
    val entryProvider = remember(navigator) { appEntryProvider(navigator) }

    Scaffold(
        // Feature screens paint their own gradient; this is only what shows behind the bar.
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
            entries = state.toEntries(entryProvider),
            onBack = navigator::goBack,
            modifier = Modifier.padding(innerPadding),
        )
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
