package com.xwab.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.navigation.rememberAppNavigationState
import com.xwab.app.ui.AppNavigationBar

/**
 * Shared application root used by the platform entry points.
 *
 * It applies the app theme and renders the application scaffold from the remembered navigation
 * state.
 */
@Composable
fun App() {
    SleepRelaxTheme {
        val navigationState = rememberAppNavigationState()
        val entryProvider = remember(navigationState) {
            appEntryProvider(
                onNavigate = navigationState::navigate,
                onBack = navigationState::goBack,
            )
        }

        Scaffold(
            // Feature screens paint their own gradient; this is only what shows behind the bar.
            containerColor = SleepRelaxTheme.colors.backgroundBottom,
            bottomBar = {
                AppNavigationBar(
                    destinations = navigationState.destinations,
                    selectedRoute = navigationState.selectedRoute,
                    onSelect = navigationState::navigate,
                )
            },
        ) { innerPadding ->
            NavDisplay(
                entries = navigationState.entries(entryProvider),
                onBack = navigationState::goBack,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
