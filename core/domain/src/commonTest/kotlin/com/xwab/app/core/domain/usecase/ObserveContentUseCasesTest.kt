package com.xwab.app.core.domain.usecase

import com.xwab.app.core.domain.port.PlaybackSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The observe use cases are the only place where catalog, favorites and playback are woven into
 * one screen model, so this is where a wrong join shows up. Every port here is a plain fake —
 * the domain layer owns no DI container, so no container is needed to drive it.
 */
class ObserveContentUseCasesTest {
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

    @Test
    fun aCategoryScreenSeesItsOwnTracksOnly() = runBlocking {
        val favorites = FakeFavorites(setOf("calm-waves"))
        val useCase = ObserveCategoryContentUseCase(catalog, favorites, FakePlaybackCoordinator())

        val content = useCase("ocean").first()

        assertEquals("ocean", content.category?.id)
        assertEquals(listOf(waves), content.musics)
        assertEquals(setOf("calm-waves"), content.favoriteIds)
    }

    @Test
    fun anUnknownCategoryYieldsNoCategoryAndNoTracks() = runBlocking {
        val useCase = ObserveCategoryContentUseCase(catalog, FakeFavorites(), FakePlaybackCoordinator())

        val content = useCase("no-such-category").first()

        assertNull(content.category)
        assertTrue(content.musics.isEmpty())
    }

    @Test
    fun thePlayerScreenCombinesTrackFavoritesPlaybackAndSleepTimer() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
        coordinator.publish(PlaybackSummary(activeSourceId = "gentle-rain", isPlaying = true))
        coordinator.publishSleepTimer(90_000L)
        val useCase = ObservePlayerContentUseCase(
            catalog,
            FakeFavorites(setOf("gentle-rain")),
            coordinator,
        )

        val content = useCase("gentle-rain").first()

        assertEquals(rain, content.music)
        assertEquals(setOf("gentle-rain"), content.favoriteIds)
        assertTrue(content.playback.isPlaying)
        assertEquals(90_000L, content.sleepTimerRemainingMs)
    }

    @Test
    fun thePlayerScreenReportsAnUnknownTrackAsMissingInsteadOfFailing() = runBlocking {
        val useCase = ObservePlayerContentUseCase(catalog, FakeFavorites(), FakePlaybackCoordinator())

        val content = useCase("no-such-track").first()

        assertNull(content.music)
        assertNull(content.sleepTimerRemainingMs)
    }
}
