package com.xwab.app.di

import com.xwab.app.core.domain.usecase.ToggleFavoriteUseCase
import com.xwab.app.core.domain.usecase.ToggleMusicPlaybackUseCase
import org.koin.dsl.module

/**
 * Wiring for the use cases more than one feature performs.
 *
 * Lives in the composition root rather than in `core:domain` on purpose: the domain layer is
 * plain Kotlin and knows no DI container, so the framework stays replaceable and a test can
 * construct any use case by hand without a container at all.
 *
 * A screen's own use case is bound by that screen's feature module instead, which is why adding
 * a feature no longer touches this file.
 *
 * The adapter modules (`audioContentModule`, `favoritesModule`, `playbackCoordinatorModule`) keep
 * owning their own wiring, because the implementations they bind are `internal` — publishing them
 * just to register them here would widen those modules' API surface for nothing.
 */
internal val domainModule = module {
    factory { ToggleFavoriteUseCase(get()) }
    factory { ToggleMusicPlaybackUseCase(get(), get()) }
}
