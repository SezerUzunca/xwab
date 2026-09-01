package com.xwab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.tab_browse
import xwab.shared.generated.resources.tab_favorites
import xwab.shared.generated.resources.tab_stories

/**
 * The app's top-level destinations, in the order the navigation bar shows them.
 *
 * List order is visible and intentional: Browse is first, and therefore the tab the app starts
 * from and falls back to on back.
 */
enum class AppTab(
    val label: @Composable () -> String,
    val icon: @Composable () -> Unit,
) {
    BROWSE(
        label = { stringResource(Res.string.tab_browse) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
    ),
    FAVORITES(
        label = { stringResource(Res.string.tab_favorites) },
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
    ),
    STORIES(
        label = { stringResource(Res.string.tab_stories) },
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
    ),
}
