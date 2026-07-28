package com.xwab.app.di

import com.xwab.app.core.audiocontent.di.audioContentModule
import com.xwab.app.core.audiocontent.di.audioContentPlatformModule
import com.xwab.app.core.favorites.di.favoritesModule
import com.xwab.app.core.media.di.playbackModule
import com.xwab.app.core.playback.di.playbackCoordinatorModule
import com.xwab.app.core.preferences.di.dataStoreModule
import com.xwab.app.feature.category.di.categoryModule
import com.xwab.app.feature.home.di.homeModule
import com.xwab.app.feature.player.di.playerModule
import org.koin.core.module.Module

fun appModules(): List<Module> = listOf(
    audioContentModule,
    audioContentPlatformModule,
    favoritesModule,
    playbackCoordinatorModule,
    domainModule,
    homeModule,
    categoryModule,
    playerModule,
    playbackModule,
    dataStoreModule,
)
