package com.xwab.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.navigation.TOP_LEVEL_DESTINATIONS
import org.jetbrains.compose.resources.stringResource

/**
 * The production navigation layout, also used by the composition tests.
 *
 * Material's [NavigationSuiteScaffold] picks the bar or rail from the window size class, as Material
 * recommends. The chrome is the same on every screen, so it lives outside [NavDisplay]: only the
 * content transitions, and there is one bar, one rail and one [player] by construction. Platform
 * Back is owned by [NavDisplay]; adaptive list, detail and extra panes come from Material's
 * list-detail scene strategy.
 *
 * Status-bar insets are left to each screen, so its background reaches the top edge. The scaffold
 * consumes the side of whichever navigation component it shows; the rest is padded here.
 */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongParameterList") // Tabs, entries, Back and the player slot are independent contracts.
internal fun AppNavigationDisplay(
    entries: List<NavEntry<NavKey>>,
    selectedTab: NavKey,
    onSelectTab: (NavKey) -> Unit,
    onBack: () -> Unit,
    player: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = SleepRelaxTheme.colors.backgroundBottom
    // The scaffold and its items take the navigation type from the current window by default.
    NavigationSuiteScaffold(
        navigationItems = {
            TOP_LEVEL_DESTINATIONS.forEach { destination ->
                NavigationSuiteItem(
                    selected = destination.route == selectedTab,
                    onClick = { onSelectTab(destination.route) },
                    // The visible label is the item's accessible name, so the icon adds nothing.
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        modifier = modifier,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = background,
            wideNavigationRailColors = WideNavigationRailDefaults.colors(containerColor = background),
        ),
        containerColor = background,
    ) {
        val unconsumedSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(unconsumedSides))) {
            AppNavDisplay(entries, onBack, Modifier.weight(1f))
            player()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun AppNavDisplay(entries: List<NavEntry<NavKey>>, onBack: () -> Unit, modifier: Modifier) {
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
    )
    NavDisplay(
        entries = entries,
        onBack = onBack,
        modifier = modifier,
        sceneStrategies = listOf(listDetailStrategy),
        transitionSpec = { navigationTransition(forward = true, direction = direction) },
        popTransitionSpec = { navigationTransition(forward = false, direction = direction) },
        predictivePopTransitionSpec = { edge ->
            navigationTransition(
                forward = false,
                direction = if (edge == NavigationEvent.EDGE_RIGHT) -1 else 1,
            )
        },
    )
}
