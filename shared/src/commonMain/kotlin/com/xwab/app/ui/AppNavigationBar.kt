package com.xwab.app.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.navigation.TopLevelDestination

/** Application chrome for switching between app-owned top-level destinations. */
@Composable
internal fun AppNavigationBar(
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
