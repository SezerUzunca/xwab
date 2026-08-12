package com.xwab.app.feature.favorites.impl.domain

import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackSummary
import com.xwab.app.core.testing.FakeFavorites
import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.core.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class ObserveFavoritesContentUseCaseTest {
    private val rain = track("gentle-rain", categoryId = "rain")
    private val waves = track("calm-waves", categoryId = "ocean")
    private val catalog = FakeMusicCatalog(tracks = listOf(rain, waves))

    @Test
    fun filtersFavoritesAndPreservesCatalogOrder() = runBlocking {
        val useCase = ObserveFavoritesContentUseCase(
            catalog,
            FakeFavorites(setOf(TrackId("calm-waves"), TrackId("gentle-rain"))),
            FakePlaybackCoordinator(),
        )

        assertEquals(listOf(rain, waves), useCase().first().musics)
    }

    @Test
    fun reflectsFavoriteChangesAfterTheFirstRead() = runBlocking {
        val favorites = FakeFavorites()
        val useCase = ObserveFavoritesContentUseCase(catalog, favorites, FakePlaybackCoordinator())
        val emissions = mutableListOf<FavoritesContent>()
        val collection = launch { useCase().take(2).toList(emissions) }
        while (emissions.isEmpty()) yield()

        assertTrue(emissions.single().musics.isEmpty())
        favorites.toggle(TrackId("calm-waves"))
        collection.join()

        assertEquals(listOf(waves), emissions.last().musics)
    }

    @Test
    fun carriesPlaybackSummaryThrough() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
        val playing = PlaybackSummary(
            requestedItemId = PlaybackItemId.sound("gentle-rain"),
            playIntent = true,
            isPlaying = true,
        )
        coordinator.publish(playing)
        val useCase = ObserveFavoritesContentUseCase(catalog, FakeFavorites(), coordinator)

        assertEquals(playing, useCase().first().playback)
    }
}
