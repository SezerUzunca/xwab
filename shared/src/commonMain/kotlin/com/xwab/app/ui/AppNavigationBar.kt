package com.xwab.app.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.navigation.AppTab

/** Application chrome for switching between app-owned top-level destinations. */
@Composable
internal fun AppNavigationBar(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    NavigationBar(containerColor = SleepRelaxTheme.colors.backgroundBottom) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                icon = tab.icon,
                label = { Text(tab.label()) },
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
