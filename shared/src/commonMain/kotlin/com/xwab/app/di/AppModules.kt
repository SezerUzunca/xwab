package com.xwab.app.di

import com.xwab.app.core.audiocontent.di.audioContentModule
import com.xwab.app.core.audiocontent.di.audioContentPlatformModule
import com.xwab.app.core.media.di.playbackModule
import com.xwab.app.core.playback.di.playbackCoordinatorModule
import com.xwab.app.core.preferences.di.preferencesModule
import com.xwab.app.core.preferences.di.preferencesPlatformModule
import org.koin.core.module.Module

/** The capability modules: one per adapter. */
internal val coreModules: List<Module> = listOf(
    audioContentModule,
    audioContentPlatformModule,
    preferencesModule,
    preferencesPlatformModule,
    playbackCoordinatorModule,
    playbackModule,
)

fun appModules(): List<Module> = coreModules + features.map { it.koinModule }
