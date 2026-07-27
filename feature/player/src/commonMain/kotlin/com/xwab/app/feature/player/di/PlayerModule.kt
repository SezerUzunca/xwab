@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.player.di

import com.xwab.app.feature.player.PlayerScreenRoute
import com.xwab.app.feature.player.PlayerViewModel
import com.xwab.app.feature.player.navigation.PlayerRoute
import com.xwab.app.core.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val playerModule = module {
    viewModel { parameters ->
        PlayerViewModel(
            musicId = parameters.get(),
            observePlayerContentUseCase = get(),
            toggleFavoriteUseCase = get(),
            toggleMusicPlaybackUseCase = get(),
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
