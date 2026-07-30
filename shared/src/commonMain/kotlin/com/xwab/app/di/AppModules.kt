package com.xwab.app.di

import com.xwab.app.core.data.di.dataModule
import com.xwab.app.core.data.di.dataPlatformModule
import com.xwab.app.core.media.di.playbackModule
import com.xwab.app.core.playback.di.playbackCoordinatorModule
import com.xwab.app.core.preferences.di.dataStoreModule
import org.koin.core.module.Module

/** The capability modules: one per adapter. */
internal val coreModules: List<Module> = listOf(
    dataModule,
    dataPlatformModule,
    playbackCoordinatorModule,
    playbackModule,
    dataStoreModule,
)

fun appModules(): List<Module> = coreModules + features.map { it.koinModule }
