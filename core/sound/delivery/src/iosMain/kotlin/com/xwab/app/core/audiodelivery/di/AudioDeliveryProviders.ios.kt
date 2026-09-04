@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiodelivery.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The iOS caches directory this app is allowed to fill. */
@ContributesTo(AppScope::class)
interface AudioDeliveryIosProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioCacheRoot(): AudioCacheRoot {
        val cachesDirectory = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSCachesDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path,
        )
        return AudioCacheRoot("$cachesDirectory/audio-content".toPath())
    }
}
