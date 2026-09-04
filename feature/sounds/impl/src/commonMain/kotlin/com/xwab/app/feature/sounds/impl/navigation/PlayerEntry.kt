package com.xwab.app.feature.sounds.impl.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.sounds.api.navigation.PlayerRoute
import com.xwab.app.feature.sounds.impl.PlayerScreenRoute
import com.xwab.app.feature.sounds.impl.PlayerViewModel
import com.xwab.app.feature.sounds.impl.di.PlayerDependencies
import com.xwab.app.feature.sounds.impl.domain.ObservePlayerContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.playerEntry(
    dependencies: PlayerDependencies,
    onBack: () -> Unit,
) {
    entry<PlayerRoute> { route ->
        PlayerScreenRoute(
            onBack = onBack,
            viewModel = viewModel {
                PlayerViewModel(
                    // A route is a serialized wire format, so it carries the plain id and the
                    // wrapper goes back on here — the one place this feature handles a bare
                    // track string.
                    trackId = TrackId(route.musicId),
                    observePlayerContentUseCase = ObservePlayerContentUseCase(
                        dependencies.musicCatalog,
                        dependencies.favoritesRepository,
                        dependencies.playbackCoordinator,
                    ),
                    favoritesRepository = dependencies.favoritesRepository,
                    playbackCoordinator = dependencies.playbackCoordinator,
                )
            },
        )
    }
}
