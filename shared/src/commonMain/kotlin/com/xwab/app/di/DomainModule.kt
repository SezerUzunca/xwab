package com.xwab.app.di

import com.xwab.app.core.domain.usecase.CancelSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ObserveCategoryContentUseCase
import com.xwab.app.core.domain.usecase.ObserveHomeContentUseCase
import com.xwab.app.core.domain.usecase.ObservePlayerContentUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackLoopingUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackVolumeUseCase
import com.xwab.app.core.domain.usecase.StartSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ToggleFavoriteUseCase
import com.xwab.app.core.domain.usecase.ToggleMusicPlaybackUseCase
import org.koin.dsl.module

/**
 * Wiring for the use cases.
 *
 * Lives in the composition root rather than in `core:domain` on purpose: the domain layer is
 * plain Kotlin and knows no DI container, so the framework stays replaceable and a test can
 * construct any use case by hand without a container at all.
 *
 * The adapter modules (`dataModule`, `playbackCoordinatorModule`) keep owning their own wiring,
 * because the implementations they bind are `internal` — publishing them just to register them
 * here would widen those modules' API surface for nothing.
 */
internal val domainModule = module {
    factory { ObserveHomeContentUseCase(get(), get(), get()) }
    factory { ObserveCategoryContentUseCase(get(), get(), get()) }
    factory { ObservePlayerContentUseCase(get(), get(), get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ToggleMusicPlaybackUseCase(get(), get()) }
    factory { SetPlaybackLoopingUseCase(get()) }
    factory { SetPlaybackVolumeUseCase(get()) }
    factory { StartSleepTimerUseCase(get()) }
    factory { CancelSleepTimerUseCase(get()) }
}
