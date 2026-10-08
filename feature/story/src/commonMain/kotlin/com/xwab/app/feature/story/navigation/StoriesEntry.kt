package com.xwab.app.feature.story.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.StoriesScreenRoute
import com.xwab.app.feature.story.StoryDetailScreenRoute
import com.xwab.app.feature.story.StoryDetailViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.Flow

/** Actions supplied by the application's composition root. */
class StoriesEntryCallbacks(
    val onStoryClick: (StoryId) -> Unit,
    val onBack: () -> Unit,
    val reselectEvents: Flow<Unit>,
)

@ContributesTo(EntryProviderScope::class)
@BindingContainer
object StoriesEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: StoriesEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<StoriesRoute> {
            StoriesScreenRoute(
                onStoryClick = callbacks.onStoryClick,
                reselectEvents = callbacks.reselectEvents,
                viewModel = metroViewModel(),
            )
        }
        entry<StoryRoute> { route ->
            StoryDetailScreenRoute(
                onBack = callbacks.onBack,
                viewModel = assistedMetroViewModel<StoryDetailViewModel, StoryDetailViewModel.Factory> {
                    create(StoryId(route.storyId))
                },
            )
        }
    }
}
