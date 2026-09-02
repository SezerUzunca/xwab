package com.xwab.app.di

import com.xwab.app.core.audiodelivery.di.audioDeliveryModule
import com.xwab.app.core.audiodelivery.di.audioDeliveryPlatformModule
import com.xwab.app.core.catalogmanifest.di.catalogManifestModule
import com.xwab.app.core.favorites.di.favoritesModule
import com.xwab.app.core.favorites.di.favoritesPlatformModule
import com.xwab.app.core.network.di.networkModule
import com.xwab.app.core.playbackengine.di.playbackModule
import com.xwab.app.core.playbacksession.di.playbackSessionModule
import com.xwab.app.core.storymanifest.di.storyManifestModule
import com.xwab.app.feature.category.impl.di.categoryModule
import com.xwab.app.feature.favorites.impl.di.favoritesFeatureModule
import com.xwab.app.feature.sounds.impl.di.soundsModule
import com.xwab.app.feature.story.impl.di.storyModule
import org.koin.core.module.Module

/** The capability modules: one per adapter. */
internal val coreModules: List<Module> = listOf(
    networkModule,
    catalogManifestModule,
    audioDeliveryModule,
    audioDeliveryPlatformModule,
    storyManifestModule,
    favoritesModule,
    favoritesPlatformModule,
    playbackSessionModule,
    playbackModule,
)

/**
 * Feature implementations assembled explicitly by the application composition root.
 *
 * Browse has no module of its own: `DefaultBrowseComponent` reads only `MusicCatalogRepository`,
 * a core port bound in [coreModules], so there is no feature-owned binding to list here.
 */
internal val featureModules: List<Module> = listOf(
    favoritesFeatureModule,
    categoryModule,
    soundsModule,
    storyModule,
)

/** Core adapters plus every feature's bindings. */
fun appModules(): List<Module> = coreModules + featureModules
