package com.xwab.app.composition

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.feature.browse.impl.navigation.browseEntry
import com.xwab.app.feature.category.api.navigation.CategoryRoute
import com.xwab.app.feature.category.impl.navigation.categoryEntry
import com.xwab.app.feature.favorites.impl.navigation.favoritesEntry
import com.xwab.app.feature.sounds.api.navigation.PlayerRoute
import com.xwab.app.feature.sounds.impl.navigation.playerEntry
import com.xwab.app.feature.story.impl.navigation.storiesEntry

/** Connects feature implementations to the navigation actions owned by the application root. */
internal fun appEntryProvider(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    browseEntry(onCategoryClick = { onNavigate(CategoryRoute(it.value)) })
    favoritesEntry(onMusicClick = { onNavigate(PlayerRoute(it.value)) })
    categoryEntry(
        onMusicClick = { onNavigate(PlayerRoute(it.value)) },
        onBack = onBack,
    )
    playerEntry(onBack = onBack)
    storiesEntry()
}
