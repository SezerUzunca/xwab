package com.xwab.app.core.storymanifest.di

import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.storymanifest.ManifestStoryCatalogRepository
import com.xwab.app.core.storymanifest.ManifestStoryStreamCatalog
import com.xwab.app.core.storymanifest.StoryStreamCatalog
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Binds the two ports the story manifest answers: the repository screens read, whose interface lives
 * in `core:story:catalog`, and the stream catalog the playback session reads, which is declared
 * here. The same split `core:sound:manifest` has, for the same reason.
 *
 * The shipped implementation has no lifecycle: it reads one local manifest and binds no network
 * client, refresh job, or persistence layer.
 */
@ContributesTo(AppScope::class)
interface StoryManifestProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideStoryCatalogRepository(): StoryCatalogRepository = ManifestStoryCatalogRepository()

    @Provides
    @SingleIn(AppScope::class)
    fun provideStoryStreamCatalog(): StoryStreamCatalog = ManifestStoryStreamCatalog()
}
