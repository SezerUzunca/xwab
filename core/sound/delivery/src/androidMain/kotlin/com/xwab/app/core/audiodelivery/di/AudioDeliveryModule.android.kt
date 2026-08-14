package com.xwab.app.core.audiodelivery.di

import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val audioDeliveryPlatformModule: Module = module {
    single {
        AudioCacheRoot(
            androidContext().cacheDir.resolve("audio-content").absolutePath.toPath(),
        )
    }
}
