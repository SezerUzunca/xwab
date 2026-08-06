package com.xwab.app.di

import com.xwab.app.core.audiodelivery.di.audioDeliveryModule
import com.xwab.app.core.audiodelivery.di.audioDeliveryPlatformModule
import com.xwab.app.core.catalogmanifest.di.catalogManifestModule
import com.xwab.app.core.favorites.di.favoritesModule
import com.xwab.app.core.favorites.di.favoritesPlatformModule
import com.xwab.app.core.playbackengine.di.playbackModule
import com.xwab.app.core.playbacksession.di.playbackSessionModule
import org.koin.core.module.Module

/** The capability modules: one per adapter. */
internal val coreModules: List<Module> = listOf(
    catalogManifestModule,
    audioDeliveryModule,
    audioDeliveryPlatformModule,
    favoritesModule,
    favoritesPlatformModule,
    playbackSessionModule,
    playbackModule,
)

fun appModules(): List<Module> = coreModules + features.map { it.koinModule }
