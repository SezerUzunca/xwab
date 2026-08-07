@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.sounds.di

import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.navigation.LocalNavigator
import com.xwab.app.feature.sounds.PlayerScreenRoute
import com.xwab.app.feature.sounds.PlayerViewModel
import com.xwab.app.feature.sounds.domain.ObservePlayerContentUseCase
import com.xwab.app.feature.sounds.navigation.PlayerRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal val soundsModule = module {
    // The screen's one use case is bound here; the ports it reads come from the core modules.
    // Looping, volume and the sleep timer reach the coordinator straight from the ViewModel —
    // they carry no decision, so there is nothing for a use case to own.
    factory { ObservePlayerContentUseCase(get(), get(), get()) }

    viewModel { parameters ->
        PlayerViewModel(
            trackId = parameters.get(),
            observePlayerContentUseCase = get(),
            favoritesRepository = get(),
            playbackCoordinator = get(),
        )
    }

    navigation<PlayerRoute> { route ->
        val navigator = LocalNavigator.current
        PlayerScreenRoute(
            onBack = navigator::goBack,
            viewModel = koinViewModel {
                // A route is a serialized wire format, so it carries the plain id and the wrapper
                // goes back on here — the one place this feature handles a bare track string.
                parametersOf(TrackId(route.musicId))
            },
        )
    }
}
