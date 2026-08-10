package com.xwab.app.feature.story.di

import com.xwab.app.feature.story.StoriesViewModel
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `StoriesEntry.kt`. */
internal val storyModule = module {
    // The screen's own use case is bound here; the two ports it reads come from the core modules.
    factory { ObserveStoriesContentUseCase(get(), get()) }

    viewModel {
        StoriesViewModel(
            observeStoriesContentUseCase = get(),
            playbackCoordinator = get(),
        )
    }
}
