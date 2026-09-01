package com.xwab.app.feature.story.impl.di

import com.xwab.app.feature.story.impl.domain.ObserveStoriesContentUseCase
import org.koin.dsl.module

/** Objects only. What this feature *shows* is wired in `:shared/composition`. */
val storyModule = module {
    // The screen's own use case is bound here; the two ports it reads come from the core modules.
    factory { ObserveStoriesContentUseCase(get(), get()) }
}
