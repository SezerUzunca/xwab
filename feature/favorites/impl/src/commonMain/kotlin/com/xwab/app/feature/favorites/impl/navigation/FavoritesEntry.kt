package com.xwab.app.feature.favorites.impl.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.favorites.api.navigation.FavoritesRoute
import com.xwab.app.feature.favorites.impl.FavoritesScreenRoute
import com.xwab.app.feature.favorites.impl.FavoritesViewModel
import com.xwab.app.feature.favorites.impl.di.FavoritesDependencies
import com.xwab.app.feature.favorites.impl.domain.ObserveFavoritesContentUseCase

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
                        dependencies.musicCatalog,
                        dependencies.favoritesRepository,
                        dependencies.playbackCoordinator,
                    ),
                    playbackCoordinator = dependencies.playbackCoordinator,
                )
            },
        )
    }
}
