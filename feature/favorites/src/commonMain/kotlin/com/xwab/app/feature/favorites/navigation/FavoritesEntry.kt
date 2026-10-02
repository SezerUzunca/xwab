package com.xwab.app.feature.favorites.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.favorites.FavoritesScreenRoute
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.favoritesEntry(
    onTrackClick: (TrackId) -> Unit,
    onBrowse: () -> Unit,
    reselectEvents: Flow<Unit> = emptyFlow(),
) {
    entry<FavoritesRoute> {
        FavoritesScreenRoute(
            onTrackClick = onTrackClick,
            onBrowse = onBrowse,
            reselectEvents = reselectEvents,
            viewModel = metroViewModel(),
        )
    }
}
