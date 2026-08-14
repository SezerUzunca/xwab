package com.xwab.app.core.storymanifest.di

import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.storymanifest.ManifestStoryCatalogRepository
import com.xwab.app.core.storymanifest.ManifestStoryStreamCatalog
import com.xwab.app.core.storymanifest.StoryStreamCatalog
import org.koin.dsl.module

/**
 * Binds the two ports the story manifest answers: the repository screens read, whose interface lives
 * in `core:story:catalog`, and the stream catalog the playback session reads, which is declared
 * here. The same split `core:sound:manifest` has, for the same reason.
 *
 * The shipped implementation has no lifecycle: it reads one local manifest and binds no network
 * client, refresh job, or persistence layer.
 */
val storyManifestModule = module {
    single<StoryCatalogRepository> { ManifestStoryCatalogRepository() }
    single<StoryStreamCatalog> { ManifestStoryStreamCatalog() }
}
