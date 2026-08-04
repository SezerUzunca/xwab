package com.xwab.app.core.audiocontent.di

import com.xwab.app.core.audiocontent.AndroidAudioCacheDirectory
import com.xwab.app.core.audiocontent.AndroidAudioTransport
import com.xwab.app.core.audiocontent.AudioFileStore
import com.xwab.app.core.audiocontent.CachingAudioFileStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val audioContentPlatformModule: Module = module {
    single<AudioFileStore> {
        // `noBackupFilesDir` keeps the cache out of Android auto backup, which is what these bytes
        // deserve: every one of them can be downloaded again.
        val root = androidContext().noBackupFilesDir.resolve("audio-content")
        CachingAudioFileStore(AndroidAudioCacheDirectory(root), AndroidAudioTransport(root))
    }
}
