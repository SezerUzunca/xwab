package com.xwab.app.core.catalogmanifest.di

import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.catalogmanifest.ManifestAudioSourceCatalog
import com.xwab.app.core.catalogmanifest.ManifestMusicCatalogRepository
import org.koin.dsl.module

/**
 * Binds the two ports the shipped manifest answers: the repository screens read, whose interface
 * lives in `core:catalog`, and the source catalog delivery reads, which is declared here.
 *
 * Neither holds a lifecycle: the manifest is a list, so there is nothing here to start, cancel or
 * close. The one binding in the old combined module that did — the prefetcher — went to
 * `core:audio-delivery` along with the rest of the fetching.
 */
val catalogManifestModule = module {
    single<MusicCatalogRepository> { ManifestMusicCatalogRepository() }
    single<AudioSourceCatalog> { ManifestAudioSourceCatalog() }
}
