@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.player.di

import com.xwab.app.feature.player.PlayerScreenRoute
import com.xwab.app.feature.player.PlayerViewModel
import com.xwab.app.feature.player.domain.CancelSleepTimerUseCase
import com.xwab.app.feature.player.domain.ObservePlayerContentUseCase
import com.xwab.app.feature.player.domain.SetPlaybackLoopingUseCase
import com.xwab.app.feature.player.domain.SetPlaybackVolumeUseCase
import com.xwab.app.feature.player.domain.StartSleepTimerUseCase
import com.xwab.app.feature.player.navigation.PlayerRoute
import com.xwab.app.core.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal val playerModule = module {
    // The screen's own use cases are bound here; the ports they drive come from the core modules.
    factory { ObservePlayerContentUseCase(get(), get(), get()) }
    factory { SetPlaybackLoopingUseCase(get()) }
    factory { SetPlaybackVolumeUseCase(get()) }
    factory { StartSleepTimerUseCase(get()) }
    factory { CancelSleepTimerUseCase(get()) }

    viewModel { parameters ->
        PlayerViewModel(
            musicId = parameters.get(),
            observePlayerContentUseCase = get(),
            favoritesRepository = get(),
            playbackCoordinator = get(),
            setPlaybackLoopingUseCase = get(),
            setPlaybackVolumeUseCase = get(),
            startSleepTimerUseCase = get(),
            cancelSleepTimerUseCase = get(),
        )
    }

    navigation<PlayerRoute> { route ->
        val navigator = LocalNavigator.current
        PlayerScreenRoute(
            onBack = navigator::goBack,
            viewModel = koinViewModel {
                parametersOf(route.musicId)
            },
        )
    }
}
