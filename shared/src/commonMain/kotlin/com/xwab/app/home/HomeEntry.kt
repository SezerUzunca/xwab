@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.category.navigation.navigateToCategory
import com.xwab.app.feature.player.navigation.navigateToPlayer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.tab_sounds

/**
 * How the home screen mounts into the shell.
 *
 * Home is not a feature slice. It lives in the composition root, so unlike `category`, `player` and
 * `story` it has no `FeatureEntry` — the root registers it by name, the way Now in Android's app
 * module names `forYouEntry` and `ForYouNavKey` directly.
 *
 * **What that costs.** `checkArchitecture`'s rules 1–4 only inspect modules whose Gradle path
 * starts with `:core:` or `:feature:`, and `:shared` is neither. `:shared` also already declares
 * `core:sound:delivery`, `core:playback:engine`, `core:sound:manifest` and `core:network`, because
 * binding them is what a composition root is for. So nothing stops this screen from resolving a
 * track to a URI or driving the player itself, and the build will not notice. Every other screen in
 * this app is refused that at compile time. Keep this file reading through
 * `MusicCatalogRepository`, `FavoritesRepository` and `PlaybackCoordinator` by hand, because the
 * rules cannot do it for you here.
 */
internal fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeRoute> {
        HomeScreenRoute(
            onCategoryClick = navigator::navigateToCategory,
            // The route carries a plain id; the screen deals in `TrackId`. This is the seam.
            onMusicClick = { trackId -> navigator.navigateToPlayer(trackId.value) },
            viewModel = koinViewModel(),
        )
    }
}

/**
 * Home's place in the navigation bar.
 *
 * Order 0, so this is the start destination: the tab back falls through to, and the one a back
 * press leaves the app from.
 */
internal val homeTopLevel = TopLevelDestination(
    route = HomeRoute,
    order = 0,
    label = { stringResource(Res.string.tab_sounds) },
    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
)
