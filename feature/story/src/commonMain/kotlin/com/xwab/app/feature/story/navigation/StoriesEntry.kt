package com.xwab.app.feature.story.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.story.StoriesScreenRoute
import com.xwab.app.feature.story.StoriesViewModel
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.StoryDetailScreenRoute
import com.xwab.app.feature.story.StoryDetailViewModel
import com.xwab.app.feature.story.di.StoriesDependencies
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase
import com.xwab.app.feature.story.domain.ObserveStoryContentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Where this feature's routes turn into screens.
 *
 * @param onSleepTimerClick a story's detail asks for the session's timer; where it lives is the
 *   app's decision.
 */
fun EntryProviderScope<NavKey>.storiesEntry(
    dependencies: () -> StoriesDependencies,
    onStoryClick: (StoryId) -> Unit,
    onBack: () -> Unit,
    onSleepTimerClick: () -> Unit,
    reselectEvents: Flow<Unit> = emptyFlow(),
) {
    entry<StoriesRoute> {
        StoriesScreenRoute(
            onStoryClick = onStoryClick,
            reselectEvents = reselectEvents,
            viewModel = viewModel {
                val ports = dependencies()
                StoriesViewModel(
                    observeStoriesContentUseCase = ObserveStoriesContentUseCase(
                        ports.storyPort,
                        ports.playbackPort,
                    ),
                    playbackPort = ports.playbackPort,
                )
            },
        )
    }
    entry<StoryRoute> { route ->
        StoryDetailScreenRoute(
            onBack = onBack,
            onSleepTimerClick = onSleepTimerClick,
            viewModel = viewModel {
                val ports = dependencies()
                StoryDetailViewModel(
                    storyId = StoryId(route.storyId),
                    observeStoryContentUseCase = ObserveStoryContentUseCase(ports.storyPort, ports.playbackPort),
                    playbackPort = ports.playbackPort,
                )
            },
        )
    }
}
