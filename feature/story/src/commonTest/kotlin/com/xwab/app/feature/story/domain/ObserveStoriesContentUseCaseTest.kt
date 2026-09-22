package com.xwab.app.feature.story.domain

import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.testing.FakePlaybackPort
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
        val coordinator = FakePlaybackPort()
        coordinator.publish(
            PlaybackSummary(
                requestedItemId = PlaybackItemId(STORY_PLAYBACK_KIND, "night-came-slowly"),
                playIntent = true,
                isPlaying = true,
            ),
        )
        coordinator.publishSleepTimer(15 * 60_000L)
        val useCase = ObserveStoriesContentUseCase(catalog, coordinator)

        val content = useCase().first()

        assertEquals(listOf(nightCameSlowly, idleFellow), content.stories)
        assertEquals(PlaybackItemId(STORY_PLAYBACK_KIND, "night-came-slowly"), content.playback.requestedItemId)
        assertTrue(content.playback.isPlaying)
        assertEquals(15 * 60_000L, content.sleepTimerRemainingMs)
    }

    @Test
    fun anEmptyCatalogIsAnEmptyScreenRatherThanAFailure() = runBlocking {
        val useCase = ObserveStoriesContentUseCase(FakeStoryCatalog(), FakePlaybackPort())

        val content = useCase().first()

        assertTrue(content.stories.isEmpty())
    }

    /**
     * A sound and a story may share a raw id, so the session publishes the kind alongside it. The
     * screen reads `requestedValueOf(STORY)`; this pins the value it reads that from.
     */
    @Test
    fun aSoundInTheSessionReachesThisScreenAsASoundAndNotAsAStory() = runBlocking {
        val coordinator = FakePlaybackPort()
        coordinator.publish(
            PlaybackSummary(requestedItemId = PlaybackItemId(OTHER_KIND, "night-came-slowly"), playIntent = true),
        )
        val useCase = ObserveStoriesContentUseCase(catalog, coordinator)

        val content = useCase().first()

        assertEquals(PlaybackItemId(OTHER_KIND, "night-came-slowly"), content.playback.requestedItemId)
    }
}

/** Some kind this screen does not show, to prove it ignores one. */
private const val OTHER_KIND = "other-kind"
