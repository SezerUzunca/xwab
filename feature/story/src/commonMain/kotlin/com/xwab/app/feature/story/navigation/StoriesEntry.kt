package com.xwab.app.feature.story.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.StoriesScreenRoute
import com.xwab.app.feature.story.StoriesViewModel
import com.xwab.app.feature.story.di.StoryDependencies
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.storiesEntry(dependencies: StoryDependencies) {
    entry<StoriesRoute> {
        StoriesScreenRoute(
            viewModel = viewModel {
                StoriesViewModel(
                    observeStoriesContentUseCase = ObserveStoriesContentUseCase(
                        dependencies.storyCatalogPort,
                        dependencies.playbackPort,
                    ),
                    playbackPort = dependencies.playbackPort,
                )
            },
        )
    }
}
