package com.xwab.app.feature.category.domain

import com.xwab.app.core.testing.FakeFavorites
import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.core.testing.category
import com.xwab.app.core.testing.track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
}
