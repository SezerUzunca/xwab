package com.xwab.app.feature.favorites.impl.di

import com.xwab.app.feature.favorites.impl.domain.ObserveFavoritesContentUseCase
import org.koin.dsl.module

val favoritesFeatureModule = module {
    factory { ObserveFavoritesContentUseCase(get(), get(), get()) }
}
