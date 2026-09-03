package com.xwab.app.di

import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.story.StoryCatalogRepository
import kotlin.test.Test
import org.koin.dsl.module
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Verifies every feature definition without constructing platform adapters or ViewModels. */
class FeatureKoinGraphTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyFeatureConstructorDependencyIsPresent() {
        val graph = module {
            includes(featureModules)

            // Feature ports are supplied by core adapter modules at runtime. Typed placeholders
            // make their contracts part of verification without instantiating those adapters.
            single<MusicCatalogRepository> { error("verification placeholder") }
            single<StoryCatalogRepository> { error("verification placeholder") }
            single<FavoritesRepository> { error("verification placeholder") }
            single<PlaybackCoordinator> { error("verification placeholder") }
        }

        graph.verify(
            // Category and Player ViewModels receive their route's id through parametersOf(...).
            // Both are wrapper types now: a bare String is no longer handed to any ViewModel, so
            // listing one here would only hide a definition that had gone missing.
            extraTypes = listOf(CategoryId::class, TrackId::class),
        )
    }
}
