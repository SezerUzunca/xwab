package com.xwab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import org.jetbrains.compose.resources.StringResource
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.tab_browse
import xwab.shared.generated.resources.tab_favorites
import xwab.shared.generated.resources.tab_stories

/**
 * App-owned metadata for one root destination in the navigation bar.
 *
 * Which label and which icon belongs to a tab is application policy, so it is decided here. How
 * either one is drawn is not: this class carries the resource and the vector, and
 * [com.xwab.app.ui.AppNavigationBar] is the only place that emits a composable from them.
 */
internal class TopLevelDestination(
    val route: NavKey,
    val label: StringResource,
    val icon: ImageVector,
)

/**
 * The navigation bar and start destination are application policy, not feature policy.
 *
 * List order is visible and intentional: Browse is the first route and therefore the route the
 * app starts from and falls back to on back.
 *
 * This list is also what [rememberNavigationState] builds a back stack per, so a route is a tab
 * exactly by appearing here — there is no second place that decides it.
 */
internal val TOP_LEVEL_DESTINATIONS: List<TopLevelDestination> = listOf(
    TopLevelDestination(
        route = BrowseRoute,
        label = Res.string.tab_browse,
        icon = Icons.Filled.Home,
    ),
    TopLevelDestination(
        route = FavoritesRoute,
        label = Res.string.tab_favorites,
        icon = Icons.Filled.Favorite,
    ),
    TopLevelDestination(
        route = StoriesRoute,
        label = Res.string.tab_stories,
        icon = Icons.AutoMirrored.Filled.List,
    ),
)
