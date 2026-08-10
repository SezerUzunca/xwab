package com.xwab.app.feature.sounds.di

import com.xwab.app.feature.sounds.SoundsViewModel
import com.xwab.app.feature.sounds.domain.ObserveSoundsContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `SoundsEntry.kt`. */
internal val soundsModule = module {
    // The screen's own use case is bound here; the ports it reads come from the core modules.
    factory { ObserveSoundsContentUseCase(get(), get(), get()) }

    viewModel {
        SoundsViewModel(
            observeSoundsContentUseCase = get(),
            playbackCoordinator = get(),
        )
    }
}
