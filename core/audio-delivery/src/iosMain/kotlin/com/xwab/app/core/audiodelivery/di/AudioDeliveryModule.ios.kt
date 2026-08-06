@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiodelivery.di

import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.audiodelivery.cache.CachingAudioFileStore
import com.xwab.app.core.audiodelivery.platform.IosAudioCacheDirectory
import com.xwab.app.core.audiodelivery.platform.IosAudioTransport
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val audioDeliveryPlatformModule: Module = module {
    single<AudioFileStore> {
        // Application Support rather than Caches: the system may empty Caches under storage
        // pressure, and a track that vanishes mid-flight costs offline playback. The directory is
        // excluded from backup instead, which is what Apple asks for re-downloadable media.
        val applicationSupport = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path,
        )
        val root = "$applicationSupport/audio-content"
        CachingAudioFileStore(IosAudioCacheDirectory(root), IosAudioTransport(root), get())
    }
}
