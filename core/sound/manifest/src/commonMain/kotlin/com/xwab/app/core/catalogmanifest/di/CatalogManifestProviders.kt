package com.xwab.app.core.catalogmanifest.di

import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.catalogmanifest.ManifestAudioSourceCatalog
import com.xwab.app.core.catalogmanifest.ManifestMusicCatalogRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Binds the two ports the shipped manifest answers: the repository screens read, whose interface
 * lives in `core:sound:catalog`, and the source catalog delivery reads, which is declared here.
 *
 * Neither holds a lifecycle: the manifest is a list, so there is nothing here to start, cancel or
 * close. The one binding in the old combined module that did — the prefetcher — went to
 * `core:sound:delivery` along with the rest of the fetching.
 */
@ContributesTo(AppScope::class)
interface CatalogManifestProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideMusicCatalogRepository(): MusicCatalogRepository = ManifestMusicCatalogRepository()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioSourceCatalog(): AudioSourceCatalog = ManifestAudioSourceCatalog()
}
