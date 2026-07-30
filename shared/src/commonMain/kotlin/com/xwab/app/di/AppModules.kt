package com.xwab.app.di

import com.xwab.app.core.audiocontent.di.audioContentModule
import com.xwab.app.core.audiocontent.di.audioContentPlatformModule
import com.xwab.app.core.favorites.di.favoritesModule
import com.xwab.app.core.media.di.playbackModule
import com.xwab.app.core.playback.di.playbackCoordinatorModule
import com.xwab.app.core.preferences.di.dataStoreModule
import org.koin.core.module.Module

/** The capability modules: one per adapter, plus the use cases more than one feature performs. */
internal val coreModules: List<Module> = listOf(
    audioContentModule,
    audioContentPlatformModule,
    favoritesModule,
    playbackCoordinatorModule,
    domainModule,
    playbackModule,
    dataStoreModule,
)

fun appModules(): List<Module> = coreModules + features.map { it.koinModule }
