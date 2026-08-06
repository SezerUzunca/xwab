package com.xwab.app.core.playbackengine.di

import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbackengine.platform.createIosPlaybackController
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

actual val playbackModule: Module = module {
    single<PlaybackController> { createIosPlaybackController() } onClose { it?.release() }
}
