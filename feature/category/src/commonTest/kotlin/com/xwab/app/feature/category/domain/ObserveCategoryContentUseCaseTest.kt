package com.xwab.app.feature.category.domain

import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeMusicCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.category
import com.xwab.app.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ObserveCategoryContentUseCaseTest {
    private val rain = track("gentle-rain", categoryId = "rain")
    private val waves = track("calm-waves", categoryId = "ocean")
    private val birds = track("forest-birds", categoryId = "forest")
    private val catalog = FakeMusicCatalog(
        categories = listOf(category("rain", musicCount = 1), category("ocean", musicCount = 1)),
        tracks = listOf(rain, waves, birds),
    )

    @Test
    fun aCategoryScreenSeesItsOwnTracksOnly() = runBlocking {
        val favorites = FakeFavorites(setOf(TrackId("calm-waves")))
        val useCase = ObserveCategoryContentUseCase(catalog, favorites, FakePlaybackPort())

        val content = useCase(CategoryId("ocean")).first()

        assertEquals(CategoryId("ocean"), content.category?.id)
        assertEquals(listOf(waves), content.musics)
        assertEquals(setOf(TrackId("calm-waves")), content.favoriteIds)
    }

    @Test
    fun anUnknownCategoryYieldsNoCategoryAndNoTracks() = runBlocking {
        val useCase = ObserveCategoryContentUseCase(catalog, FakeFavorites(), FakePlaybackPort())

        val content = useCase(CategoryId("no-such-category")).first()

        assertNull(content.category)
        assertTrue(content.musics.isEmpty())
    }
}
