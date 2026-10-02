package com.xwab.app.feature.story.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.story.StoriesScreenRoute
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.StoryDetailScreenRoute
import com.xwab.app.feature.story.StoryDetailViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.storiesEntry(
    onStoryClick: (StoryId) -> Unit,
    onBack: () -> Unit,
    reselectEvents: Flow<Unit> = emptyFlow(),
) {
    entry<StoriesRoute> {
        StoriesScreenRoute(
            onStoryClick = onStoryClick,
            reselectEvents = reselectEvents,
            viewModel = metroViewModel(),
        )
    }
    entry<StoryRoute> { route ->
        StoryDetailScreenRoute(
            onBack = onBack,
            viewModel = assistedMetroViewModel<StoryDetailViewModel, StoryDetailViewModel.Factory> {
                create(StoryId(route.storyId))
            },
        )
    }
}
