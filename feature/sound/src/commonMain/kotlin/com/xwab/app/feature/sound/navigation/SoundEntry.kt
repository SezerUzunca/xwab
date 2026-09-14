package com.xwab.app.feature.sound.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.sound.SoundScreenRoute
import com.xwab.app.feature.sound.SoundViewModel
import com.xwab.app.feature.sound.di.SoundDependencies
import com.xwab.app.feature.sound.domain.ObserveSoundContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.soundEntry(
    dependencies: SoundDependencies,
    onBack: () -> Unit,
) {
    entry<SoundRoute> { route ->
        SoundScreenRoute(
            onBack = onBack,
            viewModel = viewModel {
                SoundViewModel(
                    // A route is a serialized wire format, so it carries the plain id and the
                    // wrapper goes back on here — the one place this feature handles a bare
                    // track string.
                    trackId = TrackId(route.trackId),
                    observeSoundContentUseCase = ObserveSoundContentUseCase(
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
