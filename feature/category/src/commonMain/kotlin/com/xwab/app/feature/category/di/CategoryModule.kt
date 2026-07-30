@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.category.di

import com.xwab.app.feature.category.CategoryScreenRoute
import com.xwab.app.feature.category.CategoryViewModel
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.core.navigation.LocalNavigator
import com.xwab.app.feature.player.navigation.navigateToPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal val categoryModule = module {
    // The screen's own use case is bound here; the ports it reads come from the core modules.
    factory { ObserveCategoryContentUseCase(get(), get(), get()) }

    viewModel { parameters ->
        CategoryViewModel(
            categoryId = parameters.get(),
            observeCategoryContentUseCase = get(),
            favoritesRepository = get(),
            toggleMusicPlaybackUseCase = get(),
        )
    }

    navigation<CategoryRoute> { route ->
        val navigator = LocalNavigator.current
        CategoryScreenRoute(
            onMusicClick = navigator::navigateToPlayer,
            onBack = navigator::goBack,
            viewModel = koinViewModel {
                parametersOf(route.categoryId)
            },
        )
    }
}
