package com.xwab.app.core.data.di

import com.xwab.app.core.data.AndroidAudioFileStore
import com.xwab.app.core.data.AudioFileStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataPlatformModule: Module = module {
    single<AudioFileStore> {
        AndroidAudioFileStore(androidContext().noBackupFilesDir.resolve("audio-content"))
    }
}
