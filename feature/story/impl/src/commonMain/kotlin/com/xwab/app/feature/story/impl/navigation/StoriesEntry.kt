package com.xwab.app.feature.story.impl.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.story.api.navigation.StoriesRoute
import com.xwab.app.feature.story.impl.StoriesScreenRoute
import com.xwab.app.feature.story.impl.StoriesViewModel
import com.xwab.app.feature.story.impl.di.StoryDependencies
import com.xwab.app.feature.story.impl.domain.ObserveStoriesContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.storiesEntry(dependencies: StoryDependencies) {
    entry<StoriesRoute> {
        StoriesScreenRoute(
            viewModel = viewModel {
                StoriesViewModel(
                    observeStoriesContentUseCase = ObserveStoriesContentUseCase(
                        dependencies.storyCatalog,
                        dependencies.playbackCoordinator,
                    ),
                    playbackCoordinator = dependencies.playbackCoordinator,
                )
            },
        )
    }
}
