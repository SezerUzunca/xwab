@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.category.di

import com.xwab.app.feature.category.CategoryScreenRoute
import com.xwab.app.feature.category.CategoryViewModel
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.core.navigation.LocalNavigator
import com.xwab.app.feature.player.navigation.navigateToPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val categoryModule = module {
    viewModel { parameters ->
        CategoryViewModel(
            categoryId = parameters.get(),
            observeCategoryContentUseCase = get(),
            toggleFavoriteUseCase = get(),
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
