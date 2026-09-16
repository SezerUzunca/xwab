package com.xwab.app.feature.category.domain

import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.category
import com.xwab.app.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ObserveCategoryContentUseCaseTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun playbackReusesFavoriteIdsWhileAvailabilityAndMembershipStillUpdate() = runTest {
        val favorites = FakeFavorites(setOf(waves.id))
        val playback = FakePlaybackPort()
        val emissions = mutableListOf<CategoryContent>()
        val useCase = ObserveCategoryContentUseCase(catalog, favorites, playback)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(CategoryId("ocean")).filter { it.favoritesReadStatus != CategoryFavoritesReadStatus.Pending }.toList(emissions)
        }
        runCurrent()
        val favoriteIds = emissions.single().favoriteIds

        playback.publish(PlaybackSummary(volume = 0.5f))
        runCurrent()
        assertEquals(0.5f, emissions.last().playback.volume)
        assertSame(favoriteIds, emissions.last().favoriteIds)

        favorites.available.value = false
        runCurrent()
        assertEquals(CategoryFavoritesReadStatus.Unavailable, emissions.last().favoritesReadStatus)

        favorites.available.value = true
        favorites.toggle(SOUND_FAVORITES_NAMESPACE, waves.id.value)
        runCurrent()
        assertEquals(CategoryFavoritesReadStatus.Available, emissions.last().favoritesReadStatus)
        assertTrue(emissions.last().favoriteIds.isEmpty())
    }

    private val rain = track("gentle-rain", categoryId = "rain")
    private val waves = track("calm-waves", categoryId = "ocean")
    private val birds = track("forest-birds", categoryId = "forest")
    private val catalog = FakeSoundCatalog(
        categories = listOf(category("rain", trackCount = 1), category("ocean", trackCount = 1)),
        tracks = listOf(rain, waves, birds),
    )

    @Test
    fun aCategoryScreenSeesItsOwnTracksOnly() = runBlocking {
        val favorites = FakeFavorites(setOf(TrackId("calm-waves"), rain.id, birds.id))
        val useCase = ObserveCategoryContentUseCase(catalog, favorites, FakePlaybackPort())

        val content = useCase(CategoryId("ocean")).first { it.favoritesReadStatus == CategoryFavoritesReadStatus.Available }

        assertEquals(CategoryId("ocean"), content.category?.id)
        assertEquals(listOf(waves), content.tracks)
        assertEquals(setOf(TrackId("calm-waves")), content.favoriteIds)
    }

    @Test
    fun anUnknownCategoryYieldsNoCategoryAndNoTracks() = runBlocking {
        val useCase = ObserveCategoryContentUseCase(catalog, FakeFavorites(), FakePlaybackPort())

        val content = useCase(CategoryId("no-such-category")).first()

        assertNull(content.category)
        assertTrue(content.tracks.isEmpty())
    }
}
