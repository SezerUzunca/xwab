@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.data.di

import com.xwab.app.core.data.AudioFileStore
import com.xwab.app.core.data.IosAudioFileStore
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val dataPlatformModule: Module = module {
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
