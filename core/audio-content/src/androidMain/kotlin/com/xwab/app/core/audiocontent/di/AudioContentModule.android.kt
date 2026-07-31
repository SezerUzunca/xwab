package com.xwab.app.core.audiocontent.di

import com.xwab.app.core.audiocontent.AndroidAudioFileStore
import com.xwab.app.core.audiocontent.AudioFileStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val audioContentPlatformModule: Module = module {
    single<AudioFileStore> {
        AndroidAudioFileStore(androidContext().noBackupFilesDir.resolve("audio-content"))
    }
}
