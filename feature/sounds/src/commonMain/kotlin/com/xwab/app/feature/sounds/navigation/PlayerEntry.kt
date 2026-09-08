package com.xwab.app.feature.sounds.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.sounds.navigation.PlayerRoute
import com.xwab.app.feature.sounds.PlayerScreenRoute
import com.xwab.app.feature.sounds.PlayerViewModel
import com.xwab.app.feature.sounds.di.PlayerDependencies
import com.xwab.app.feature.sounds.domain.ObservePlayerContentUseCase

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
                        dependencies.soundPort,
                        dependencies.favoritesPort,
                        dependencies.playbackPort,
                    ),
                    favoritesPort = dependencies.favoritesPort,
                    playbackPort = dependencies.playbackPort,
                )
            },
        )
    }
}
