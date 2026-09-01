package com.xwab.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.xwab.app.composition.AppComponent
import com.xwab.app.composition.AppContent
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.ui.AppNavigationBar

/**
 * Shared application root used by the platform entry points.
 *
 * It applies the app theme and renders the application scaffold from [root]. The platform entry
 * point (`MainActivity`/`MainViewController`) builds [root] once, outside the composition, so it
 * survives configuration changes rather than being rebuilt on every recomposition.
 */
@Composable
fun App(root: AppComponent) {
    SleepRelaxTheme {
        val selectedTab by root.selectedTab.subscribeAsState()

        Scaffold(
            // Feature screens paint their own gradient; this is only what shows behind the bar.
            containerColor = SleepRelaxTheme.colors.backgroundBottom,
            bottomBar = {
                AppNavigationBar(
                    selectedTab = selectedTab,
                    onSelect = root::selectTab,
                )
            },
        ) { innerPadding ->
            AppContent(
                component = root,
                selectedTab = selectedTab,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
