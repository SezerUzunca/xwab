package com.xwab.app.di

import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.story.StoryCatalogRepository
import kotlin.test.Test
import org.koin.dsl.module
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * Verifies every feature definition without constructing platform adapters.
 *
 * Feature components are never Koin-managed — `:shared/composition` constructs them directly, so
 * a screen's `CategoryId`/`TrackId` never passes through Koin. What stays worth verifying here is
 * each feature's own use-case bindings against the core ports they read.
 */
class FeatureKoinGraphTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyFeatureUseCaseDependencyIsPresent() {
        val graph = module {
            includes(featureModules)

            // Feature ports are supplied by core adapter modules at runtime. Typed placeholders
            // make their contracts part of verification without instantiating those adapters.
            single<MusicCatalogRepository> { error("verification placeholder") }
            single<StoryCatalogRepository> { error("verification placeholder") }
            single<FavoritesRepository> { error("verification placeholder") }
            single<PlaybackCoordinator> { error("verification placeholder") }
        }

        graph.verify()
    }
}
