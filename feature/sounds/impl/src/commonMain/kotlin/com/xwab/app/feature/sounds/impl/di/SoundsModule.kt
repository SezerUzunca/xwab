package com.xwab.app.feature.sounds.impl.di

import com.xwab.app.feature.sounds.impl.PlayerViewModel
import com.xwab.app.feature.sounds.impl.domain.ObservePlayerContentUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Objects only. What this feature *shows* is in `PlayerEntry.kt`. */
val soundsModule = module {
    // The screen's one use case is bound here; the ports it reads come from the core modules.
    // Looping, volume and the sleep timer reach the coordinator straight from the ViewModel —
    // they carry no decision, so there is nothing for a use case to own.
    factory { ObservePlayerContentUseCase(get(), get(), get()) }

    viewModel { parameters ->
        PlayerViewModel(
            trackId = parameters.get(),
            observePlayerContentUseCase = get(),
            favoritesRepository = get(),
            playbackCoordinator = get(),
        )
    }
}
