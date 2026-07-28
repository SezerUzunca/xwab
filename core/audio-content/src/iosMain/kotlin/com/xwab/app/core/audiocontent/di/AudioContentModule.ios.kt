@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiocontent.di

import com.xwab.app.core.audiocontent.AudioFileStore
import com.xwab.app.core.audiocontent.IosAudioFileStore
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val audioContentPlatformModule: Module = module {
    single<AudioFileStore> {
        val applicationSupport = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path,
        )
        IosAudioFileStore("$applicationSupport/audio-content")
    }
}
