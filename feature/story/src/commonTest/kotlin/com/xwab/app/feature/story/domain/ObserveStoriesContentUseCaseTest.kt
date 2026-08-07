package com.xwab.app.feature.story.domain

import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackSummary
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.feature.story.FakeStoryCatalog
import com.xwab.app.feature.story.story
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ObserveStoriesContentUseCaseTest {
    private val nightCameSlowly = story("night-came-slowly")
    private val idleFellow = story("an-idle-fellow")
    private val catalog = FakeStoryCatalog(listOf(nightCameSlowly, idleFellow))

    @Test
    fun theStoryScreenSeesTheWholeCatalogAndTheSession() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
        coordinator.publish(
            PlaybackSummary(
                requestedItemId = PlaybackItemId.story("night-came-slowly"),
                playIntent = true,
                isPlaying = true,
            ),
        )
        val useCase = ObserveStoriesContentUseCase(catalog, coordinator)

        val content = useCase().first()

        assertEquals(listOf(nightCameSlowly, idleFellow), content.stories)
        assertEquals(PlaybackItemId.story("night-came-slowly"), content.playback.requestedItemId)
        assertTrue(content.playback.isPlaying)
    }

    @Test
    fun anEmptyCatalogIsAnEmptyScreenRatherThanAFailure() = runBlocking {
        val useCase = ObserveStoriesContentUseCase(FakeStoryCatalog(), FakePlaybackCoordinator())

        val content = useCase().first()

        assertTrue(content.stories.isEmpty())
    }

    /**
     * A sound and a story may share a raw id, so the session publishes the kind alongside it. The
     * screen reads `requestedValueOf(STORY)`; this pins the value it reads that from.
     */
    @Test
    fun aSoundInTheSessionReachesThisScreenAsASoundAndNotAsAStory() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
        coordinator.publish(
            PlaybackSummary(requestedItemId = PlaybackItemId.sound("night-came-slowly"), playIntent = true),
        )
        val useCase = ObserveStoriesContentUseCase(catalog, coordinator)

        val content = useCase().first()

        assertEquals(PlaybackItemId.sound("night-came-slowly"), content.playback.requestedItemId)
    }
}
