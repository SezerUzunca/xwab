package com.xwab.app.feature.favorites.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.favorites.FavoritesScreenRoute
import com.xwab.app.feature.favorites.FavoritesViewModel
import com.xwab.app.feature.favorites.di.FavoritesDependencies
import com.xwab.app.feature.favorites.domain.ObserveFavoritesContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.favoritesEntry(
    dependencies: () -> FavoritesDependencies,
    onTrackClick: (TrackId) -> Unit,
) {
    entry<FavoritesRoute> {
        FavoritesScreenRoute(
            onTrackClick = onTrackClick,
            viewModel = viewModel {
                val ports = dependencies()
                FavoritesViewModel(
                    observeFavoritesContentUseCase = ObserveFavoritesContentUseCase(
                        ports.soundPort,
                        ports.favoritesPort,
                        ports.playbackPort,
                    ),
                    playbackPort = ports.playbackPort,
                )
            },
        )
    }
}
