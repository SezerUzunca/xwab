package com.xwab.app.feature.sound.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.sound.SoundDetailScreenRoute
import com.xwab.app.feature.sound.SoundViewModel
import com.xwab.app.feature.sound.di.SoundDependencies
import com.xwab.app.feature.sound.domain.ObserveSoundContentUseCase

/**
 * Where this feature's routes turn into screens.
 *
 * @param onSleepTimerClick a sound's detail asks for the session's timer; where it lives is the
 *   app's decision.
 */
fun EntryProviderScope<NavKey>.soundEntry(
    dependencies: () -> SoundDependencies,
    onBack: () -> Unit,
    onSleepTimerClick: () -> Unit,
) {
    entry<SoundRoute> { route ->
        SoundDetailScreenRoute(
            onBack = onBack,
            onSleepTimerClick = onSleepTimerClick,
            viewModel = viewModel {
                val ports = dependencies()
                SoundViewModel(
                    // A route is a serialized wire format, so it carries the plain id and the
                    // wrapper goes back on here — the one place this feature handles a bare
                    // track string.
                    trackId = TrackId(route.trackId),
                    observeSoundContentUseCase = ObserveSoundContentUseCase(
                        ports.soundPort,
                        ports.favoritesPort,
                        ports.playbackPort,
                    ),
                    favoritesPort = ports.favoritesPort,
                    playbackPort = ports.playbackPort,
                )
            },
        )
    }
}
