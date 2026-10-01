package com.xwab.app.composition

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.browse.navigation.browseEntry
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.category.navigation.categoryEntry
import com.xwab.app.feature.favorites.navigation.favoritesEntry
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.sound.navigation.soundEntry
import com.xwab.app.feature.story.navigation.storiesEntry
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import kotlinx.coroutines.flow.Flow
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.app_subtitle
import xwab.shared.generated.resources.app_title

/**
 * Connects feature entry providers to the navigation actions owned by the application root.
 *
 * Each feature receives a provider for its ports and callbacks for its navigation intents.
 * Providers are invoked inside feature ViewModel initializers, not while registering entries.
 */
internal fun appEntryProvider(
    graph: AppGraph,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onReselect: (NavKey) -> Flow<Unit>,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    browseEntry(
        dependencies = graph.browseDependencies,
        title = Res.string.app_title,
        subtitle = Res.string.app_subtitle,
        onCategoryClick = { onNavigate(CategoryRoute(it.value)) },
        reselectEvents = onReselect(BrowseRoute),
    )
    favoritesEntry(
        dependencies = graph.favoritesDependencies,
        onTrackClick = { onNavigate(SoundRoute(it.value)) },
        onBrowse = { onNavigate(BrowseRoute) },
        reselectEvents = onReselect(FavoritesRoute),
    )
    categoryEntry(
        dependencies = graph.categoryDependencies,
        onTrackClick = { onNavigate(SoundRoute(it.value)) },
        onBack = onBack,
    )
    soundEntry(
        dependencies = graph.soundDependencies,
        onBack = onBack,
    )
    storiesEntry(
        dependencies = graph.storiesDependencies,
        onStoryClick = { onNavigate(StoryRoute(it.value)) },
        reselectEvents = onReselect(StoriesRoute),
        onBack = onBack,
    )
}
