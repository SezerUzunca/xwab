package com.xwab.app.feature.story.impl.di

import com.xwab.app.feature.story.impl.StoriesViewModel
import com.xwab.app.feature.story.impl.domain.ObserveStoriesContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `StoriesEntry.kt`. */
val storyModule = module {
    // The screen's own use case is bound here; the two ports it reads come from the core modules.
    factory { ObserveStoriesContentUseCase(get(), get()) }

    viewModel {
        StoriesViewModel(
            observeStoriesContentUseCase = get(),
            playbackCoordinator = get(),
        )
    }
}
