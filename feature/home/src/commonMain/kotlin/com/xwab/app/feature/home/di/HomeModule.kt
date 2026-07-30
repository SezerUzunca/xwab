@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.home.di

import com.xwab.app.feature.home.HomeScreenRoute
import com.xwab.app.feature.home.HomeViewModel
import com.xwab.app.feature.home.domain.ObserveHomeContentUseCase
import com.xwab.app.feature.home.navigation.HomeRoute
import com.xwab.app.core.navigation.LocalNavigator
import com.xwab.app.feature.category.navigation.navigateToCategory
import com.xwab.app.feature.player.navigation.navigateToPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal val homeModule = module {
    // The screen's own use case is bound here; the ports it reads come from the core modules.
    factory { ObserveHomeContentUseCase(get(), get(), get()) }

    viewModel {
        HomeViewModel(
            observeHomeContentUseCase = get(),
            toggleMusicPlaybackUseCase = get(),
        )
    }

    navigation<HomeRoute> {
        val navigator = LocalNavigator.current
        HomeScreenRoute(
            onCategoryClick = navigator::navigateToCategory,
            onMusicClick = navigator::navigateToPlayer,
            viewModel = koinViewModel(),
        )
    }
}
