package com.xwab.app.di

import com.xwab.app.core.data.di.dataModule
import com.xwab.app.core.media.di.playbackModule
import com.xwab.app.core.domain.di.domainModule
import com.xwab.app.core.preferences.di.dataStoreModule
import com.xwab.app.feature.category.di.categoryModule
import com.xwab.app.feature.home.di.homeModule
import com.xwab.app.feature.player.di.playerModule
import org.koin.core.module.Module

fun appModules(): List<Module> = listOf(
    dataModule,
    domainModule,
    homeModule,
    categoryModule,
    playerModule,
    playbackModule,
    dataStoreModule,
)
