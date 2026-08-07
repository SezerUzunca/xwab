@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.story.di

import com.xwab.app.feature.story.StoriesScreenRoute
import com.xwab.app.feature.story.StoriesViewModel
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase
import com.xwab.app.feature.story.navigation.StoriesRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

internal val storyModule = module {
    // The screen's own use case is bound here; the two ports it reads come from the core modules.
    factory { ObserveStoriesContentUseCase(get(), get()) }

    viewModel {
        StoriesViewModel(
            observeStoriesContentUseCase = get(),
            playbackCoordinator = get(),
        )
    }

    // A tab's root: nothing above it to go back to, so it takes no navigator.
    navigation<StoriesRoute> {
        StoriesScreenRoute(viewModel = koinViewModel())
    }
}
