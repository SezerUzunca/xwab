package com.xwab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.feature.browse.api.navigation.BrowseRoute
import com.xwab.app.feature.browse.api.navigation.browseNavigationSerializers
import com.xwab.app.feature.browse.impl.navigation.browseEntry
import com.xwab.app.feature.category.api.navigation.CategoryRoute
import com.xwab.app.feature.category.api.navigation.categoryNavigationSerializers
import com.xwab.app.feature.category.impl.navigation.categoryEntry
import com.xwab.app.feature.favorites.api.navigation.FavoritesRoute
import com.xwab.app.feature.favorites.api.navigation.favoritesNavigationSerializers
import com.xwab.app.feature.favorites.impl.navigation.favoritesEntry
import com.xwab.app.feature.sounds.api.navigation.PlayerRoute
import com.xwab.app.feature.sounds.api.navigation.soundsNavigationSerializers
import com.xwab.app.feature.sounds.impl.navigation.playerEntry
import com.xwab.app.feature.story.api.navigation.StoriesRoute
import com.xwab.app.feature.story.api.navigation.storyNavigationSerializers
import com.xwab.app.feature.story.impl.navigation.storiesEntry
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.compose.resources.stringResource
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.tab_browse
import xwab.shared.generated.resources.tab_favorites
import xwab.shared.generated.resources.tab_stories

/** App-owned metadata for one root destination in the navigation bar. */
internal class TopLevelDestination(
    val route: NavKey,
    val label: @Composable () -> String,
    val icon: @Composable () -> Unit,
)

/**
 * The navigation bar and start destination are application policy, not feature policy.
 *
 * List order is visible and intentional: Browse is the first route and therefore the route the
 * app starts from and falls back to on back.
 */
internal val TOP_LEVEL_DESTINATIONS: List<TopLevelDestination> = listOf(
    TopLevelDestination(
        route = BrowseRoute,
        label = { stringResource(Res.string.tab_browse) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
    ),
    TopLevelDestination(
        route = FavoritesRoute,
        label = { stringResource(Res.string.tab_favorites) },
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
    ),
    TopLevelDestination(
        route = StoriesRoute,
        label = { stringResource(Res.string.tab_stories) },
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
    ),
)

/** The app assembles feature implementations and owns every cross-feature route. */
internal fun appEntryProvider(navigator: Navigator): (NavKey) -> NavEntry<NavKey> = entryProvider {
    browseEntry(onCategoryClick = { navigator.navigate(CategoryRoute(it.value)) })
    favoritesEntry(onMusicClick = { navigator.navigate(PlayerRoute(it.value)) })
    categoryEntry(
        onMusicClick = { navigator.navigate(PlayerRoute(it.value)) },
        onBack = navigator::goBack,
    )
    playerEntry(onBack = navigator::goBack)
    storiesEntry()
}

/** The app explicitly assembles the route serializers exported by feature APIs. */
internal val FEATURE_SERIALIZERS: SerializersModule = SerializersModule {
    include(browseNavigationSerializers)
    include(favoritesNavigationSerializers)
    include(categoryNavigationSerializers)
    include(soundsNavigationSerializers)
    include(storyNavigationSerializers)
}
