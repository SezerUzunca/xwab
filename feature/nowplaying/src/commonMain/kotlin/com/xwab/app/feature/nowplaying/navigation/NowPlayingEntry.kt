package com.xwab.app.feature.nowplaying.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.feature.nowplaying.NowPlayingScreenRoute
import com.xwab.app.feature.nowplaying.NowPlayingViewModel
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies
import com.xwab.app.feature.nowplaying.domain.ObserveNowPlayingContentUseCase

/** Registers the player with the same entry-owned presentation scope as other features. */
fun EntryProviderScope<NavKey>.nowPlayingEntry(
    dependencies: () -> NowPlayingDependencies,
    onBack: () -> Unit,
    onOpenDetails: (PlaybackItemId) -> Unit,
) {
    entry<NowPlayingRoute> {
        NowPlayingScreenRoute(
            onBack = onBack,
            onOpenDetails = onOpenDetails,
            viewModel = viewModel {
                val ports = dependencies()
                NowPlayingViewModel(ObserveNowPlayingContentUseCase(ports.playbackPort), ports.playbackPort)
            },
        )
    }
}
