package com.xwab.app.feature.home.domain

import com.xwab.app.core.domain.port.PlaybackSummary
import com.xwab.app.core.testing.FakeFavorites
import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.core.testing.category
import com.xwab.app.core.testing.track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home screen's use case is the only place where catalog, favorites and playback are woven
 * into one screen model, so this is where a wrong join shows up. Every port here is a plain fake
 * — the use case owns no DI container, so no container is needed to drive it.
 */
class ObserveHomeContentUseCaseTest {
    private val rain = track("gentle-rain", categoryId = "rain")
    private val waves = track("calm-waves", categoryId = "ocean")
    private val birds = track("forest-birds", categoryId = "forest")
    private val catalog = FakeMusicCatalog(
        categories = listOf(category("rain", musicCount = 1), category("ocean", musicCount = 1)),
        tracks = listOf(rain, waves, birds),
    )

    @Test
    fun homeShowsOnlyFavoritedTracksAndKeepsTheCatalogOrder() = runBlocking {
        val favorites = FakeFavorites(setOf("forest-birds", "gentle-rain"))
        val useCase = ObserveHomeContentUseCase(catalog, favorites, FakePlaybackCoordinator())

        val content = useCase().first()

        assertEquals(listOf(rain, birds), content.favoriteMusics)
        assertEquals(listOf("rain", "ocean"), content.categories.map { it.id })
    }

    @Test
    fun homeShowsNoFavoritesWhenNothingIsFavorited() = runBlocking {
        val useCase = ObserveHomeContentUseCase(catalog, FakeFavorites(), FakePlaybackCoordinator())

        assertTrue(useCase().first().favoriteMusics.isEmpty())
    }

    @Test
    fun homeReflectsAFavoriteAddedAfterTheFirstRead() = runBlocking {
        val favorites = FakeFavorites()
        val useCase = ObserveHomeContentUseCase(catalog, favorites, FakePlaybackCoordinator())
        assertTrue(useCase().first().favoriteMusics.isEmpty())

        favorites.toggle("calm-waves")

        assertEquals(listOf(waves), useCase().first().favoriteMusics)
    }

    @Test
    fun homeCarriesThePlaybackSummaryStraightThrough() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
        val playing = PlaybackSummary(activeSourceId = "gentle-rain", isPlaying = true, volume = 0.4f)
        coordinator.publish(playing)
        val useCase = ObserveHomeContentUseCase(catalog, FakeFavorites(), coordinator)

        assertEquals(playing, useCase().first().playback)
    }
}
