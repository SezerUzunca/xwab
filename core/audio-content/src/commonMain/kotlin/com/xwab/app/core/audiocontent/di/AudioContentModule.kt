package com.xwab.app.core.audiocontent.di

import com.xwab.app.core.audiocontent.HybridAudioContentRepository
import com.xwab.app.core.domain.port.AudioContentResolver
import com.xwab.app.core.domain.port.MusicCatalogRepository
import org.koin.dsl.module
import org.koin.dsl.onClose

val audioContentModule = module {
    single { HybridAudioContentRepository(get()) } onClose { it?.close() }
    single<MusicCatalogRepository> { get<HybridAudioContentRepository>() }
    single<AudioContentResolver> { get<HybridAudioContentRepository>() }
}
