package com.xwab.app.composition

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.browse.navigation.browseEntry
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.category.navigation.categoryEntry
import com.xwab.app.feature.favorites.navigation.favoritesEntry
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.sound.navigation.soundEntry
import com.xwab.app.feature.story.navigation.storiesEntry

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
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    browseEntry(
        dependencies = graph.browseDependencies,
        onCategoryClick = { onNavigate(CategoryRoute(it.value)) },
    )
    favoritesEntry(
        dependencies = graph.favoritesDependencies,
        onTrackClick = { onNavigate(SoundRoute(it.value)) },
    )
    categoryEntry(
        dependencies = graph.categoryDependencies,
        onTrackClick = { onNavigate(SoundRoute(it.value)) },
        onBack = onBack,
    )
    soundEntry(
        dependencies = graph.soundDependencies,
        onBack = onBack
    )
    storiesEntry(
        dependencies = graph.storiesDependencies
    )
}
