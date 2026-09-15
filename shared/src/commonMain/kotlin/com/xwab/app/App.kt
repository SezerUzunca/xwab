package com.xwab.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.di.AppGraph
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.TOP_LEVEL_DESTINATIONS
import com.xwab.app.navigation.rememberNavigationState
import com.xwab.app.navigation.toEntries
import com.xwab.app.ui.AppNavigationBar

/**
 * Shared application root used by the platform entry points.
 *
 * It applies the app theme and renders the application scaffold from the remembered navigation
 * state. The [graph] is built once by the platform entry point and handed down: nothing here looks
 * a dependency up.
 */
@Composable
fun App(graph: AppGraph) {
    SleepRelaxTheme {
        val navigationState = rememberNavigationState()
        val navigator = remember(navigationState) { Navigator(navigationState) }
        val entryProvider = remember(navigator, graph) {
            appEntryProvider(
                graph = graph,
                onNavigate = navigator::navigate,
                onBack = navigator::goBack,
            )
        }

        Scaffold(
            // Feature screens paint their own gradient; this is only what shows behind the bar.
            containerColor = SleepRelaxTheme.colors.backgroundBottom,
            bottomBar = {
                AppNavigationBar(
                    destinations = TOP_LEVEL_DESTINATIONS,
                    selectedRoute = navigationState.topLevelRoute,
                    onSelect = navigator::navigate,
                )
            },
        ) { innerPadding ->
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = navigator::goBack,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
