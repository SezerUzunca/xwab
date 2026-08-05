package com.xwab.app.core.media.di

import com.xwab.app.core.media.api.PlaybackController
import com.xwab.app.core.media.platform.createAndroidPlaybackController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

actual val playbackModule: Module = module {
    single<PlaybackController> { createAndroidPlaybackController(androidContext()) } onClose { it?.release() }
}
