package com.xwab.app.core.storymanifest.di

import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.storymanifest.ManifestStoryCatalogRepository
import org.koin.dsl.module

/**
 * Binds the one port the story manifest answers: the repository screens read, whose interface lives
 * in `core:story:catalog`.
 *
 * No lifecycle here — the manifest is a list, so there is nothing to start, cancel or close. When a
 * feed-backed implementation arrives it will bring a client that does have one, and this is the
 * module that gains it; nothing outside changes.
 */
val storyManifestModule = module {
    single<StoryCatalogRepository> { ManifestStoryCatalogRepository() }
}
