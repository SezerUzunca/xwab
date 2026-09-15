package com.xwab.app.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.navigation.TopLevelDestination
import org.jetbrains.compose.resources.stringResource

/**
 * Application chrome for switching between app-owned top-level destinations.
 *
 * This is the one place a [TopLevelDestination] becomes UI: the label resource and the icon vector
 * are policy owned by `com.xwab.app.navigation`, and drawing them is owned here.
 *
 * The container color matches the scaffold's in [com.xwab.app.App] on purpose — the bar has no
 * surface of its own to show.
 */
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
                // The visible label is the item's accessible name, so the icon adds nothing.
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
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
