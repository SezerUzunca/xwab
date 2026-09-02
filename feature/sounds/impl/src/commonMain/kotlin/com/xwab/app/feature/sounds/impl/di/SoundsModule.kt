package com.xwab.app.feature.sounds.impl.di

import com.xwab.app.feature.sounds.impl.domain.ObservePlayerContentUseCase
import org.koin.dsl.module

/** Objects only. What this feature *shows* is wired in `:shared/composition`. */
val soundsModule = module {
    // The screen's one use case is bound here; the ports it reads come from the core modules.
    // Looping, volume and the sleep timer reach the coordinator straight from the component —
    // they carry no decision, so there is nothing for a use case to own.
    factory { ObservePlayerContentUseCase(get(), get(), get()) }
}
