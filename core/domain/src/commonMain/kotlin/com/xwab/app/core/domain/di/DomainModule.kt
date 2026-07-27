package com.xwab.app.core.domain.di

import com.xwab.app.core.domain.usecase.ObserveCategoryContentUseCase
import com.xwab.app.core.domain.usecase.ObserveHomeContentUseCase
import com.xwab.app.core.domain.usecase.ObservePlayerContentUseCase
import com.xwab.app.core.domain.usecase.CancelSleepTimerUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackLoopingUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackVolumeUseCase
import com.xwab.app.core.domain.usecase.StartSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ToggleFavoriteUseCase
import com.xwab.app.core.domain.usecase.ToggleMusicPlaybackUseCase
import org.koin.dsl.module

val domainModule = module {
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
