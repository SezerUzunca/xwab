package com.xwab.app.home.di

import com.xwab.app.home.HomeViewModel
import com.xwab.app.home.domain.ObserveHomeContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `SoundsEntry.kt`. */
internal val homeModule = module {
    // The screen's own use case is bound here; the ports it reads come from the core modules.
    factory { ObserveHomeContentUseCase(get(), get(), get()) }

    viewModel {
        HomeViewModel(
            observeHomeContentUseCase = get(),
            playbackCoordinator = get(),
        )
    }
}
