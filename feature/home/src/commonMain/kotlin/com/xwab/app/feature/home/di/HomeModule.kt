package com.xwab.app.feature.home.di

import com.xwab.app.feature.home.HomeViewModel
import com.xwab.app.feature.home.domain.ObserveHomeContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `HomeEntry.kt`. */
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
