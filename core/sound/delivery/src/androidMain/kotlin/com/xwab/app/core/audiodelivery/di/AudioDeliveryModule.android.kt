package com.xwab.app.core.audiodelivery.di

import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.audiodelivery.cache.CachingAudioFileStore
import com.xwab.app.core.audiodelivery.platform.AndroidAudioCacheDirectory
import com.xwab.app.core.audiodelivery.platform.AndroidAudioTransport
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val audioDeliveryPlatformModule: Module = module {
    single<AudioFileStore> {
        // `noBackupFilesDir` keeps the cache out of Android auto backup, which is what these bytes
        // deserve: every one of them can be downloaded again.
        val root = androidContext().noBackupFilesDir.resolve("audio-content")
        CachingAudioFileStore(AndroidAudioCacheDirectory(root), AndroidAudioTransport(root), get())
    }
}
