package com.xwab.app.feature.favorites.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.favorites.FavoritesScreenRoute
import com.xwab.app.feature.favorites.FavoritesViewModel
import com.xwab.app.feature.favorites.di.FavoritesDependencies
import com.xwab.app.feature.favorites.domain.ObserveFavoritesContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.favoritesEntry(
    dependencies: FavoritesDependencies,
    onMusicClick: (TrackId) -> Unit,
) {
    entry<FavoritesRoute> {
        FavoritesScreenRoute(
            onMusicClick = onMusicClick,
            viewModel = viewModel {
                FavoritesViewModel(
                    observeFavoritesContentUseCase = ObserveFavoritesContentUseCase(
                        dependencies.soundCatalogPort,
                        dependencies.favoritesPort,
                        dependencies.playbackPort,
                    ),
                    playbackPort = dependencies.playbackPort,
                )
            },
        )
    }
}
