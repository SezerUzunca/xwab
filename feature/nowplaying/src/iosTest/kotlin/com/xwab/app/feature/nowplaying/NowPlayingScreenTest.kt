package com.xwab.app.feature.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Shared controls must work for every content kind, without depending on its catalog or detail screen. */
@OptIn(ExperimentalTestApi::class)
class NowPlayingScreenTest {
    @Test
    fun theMiniPlayerShowsItsTimerAndSeparatesOpeningFromPlayback() = runComposeUiTest {
        var opened = 0
        var playbackClicks = 0
        setContent {
            SleepRelaxTheme {
                NowPlayingMiniPlayer(
                    state().copy(sleepTimerRemainingMs = 900_000L),
                    onPlayPauseClick = { playbackClicks++ },
                    onOpenClick = { opened++ },
                )
            }
        }

        onNodeWithText("Stops in 15:00").assertIsDisplayed()
        assertEquals(
            "Open player and sleep timer",
            onNodeWithText(TITLE).fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
        onNodeWithText(TITLE).performClick()
        assertEquals(1, opened)
        assertEquals(0, playbackClicks)

        onNodeWithContentDescription("Pause $TITLE").performClick()
        assertEquals(1, playbackClicks)
        assertEquals(1, opened)
    }

    @Test
    fun timerTicksStayVisibleWithoutBecomingRepeatedScreenReaderAnnouncements() = runComposeUiTest {
        val content = mutableStateOf(state().copy(sleepTimerRemainingMs = 900_000L))
        setContent {
            SleepRelaxTheme { NowPlayingMiniPlayer(content.value, {}, {}) }
        }

        val timer = onNodeWithText("Stops in 15:00", useUnmergedTree = true)
        timer.assertIsDisplayed()
        assertNull(timer.fetchSemanticsNode().config.getOrNull(SemanticsProperties.LiveRegion))

        runOnIdle { content.value = content.value.copy(sleepTimerRemainingMs = 899_000L) }
        onNodeWithText("Stops in 14:59").assertIsDisplayed()
        runOnIdle { content.value = content.value.copy(sleepTimerRemainingMs = null) }
        onNodeWithText("Stops in 14:59").assertDoesNotExist()
        onNodeWithText("Playing · Timer off").assertIsDisplayed()
    }

    @Test
    fun preparingContentCanBePausedWithoutClaimingThatItIsAlreadyPlaying() = runComposeUiTest {
        var pauses = 0
        setContent {
            SleepRelaxTheme { NowPlayingMiniPlayer(state().copy(isPreparing = true), { pauses++ }, {}) }
        }

        onNodeWithText("Loading…", useUnmergedTree = true).assertExists()
        onNodeWithText("Open player and sleep timer", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Playing · Timer off").assertDoesNotExist()
        onNodeWithContentDescription("Pause $TITLE").performClick()
        assertEquals(1, pauses)
    }

    @Test
    fun aTimerWithNothingRequestedKeepsABarThatOpensThePlayer() = runComposeUiTest {
        var opened = 0
        setContent {
            SleepRelaxTheme {
                NowPlayingMiniPlayer(NowPlayingState(sleepTimerRemainingMs = 60_000L), {}, { opened++ })
            }
        }

        onNodeWithText("Stops in 1:00").assertIsDisplayed()
        onNodeWithContentDescription("Play", substring = true).assertDoesNotExist()
        onNodeWithText("Sleep timer").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun aPlaybackFailureIsAnnouncedAndOffersTheSameItemsRetry() = runComposeUiTest {
        setContent {
            SleepRelaxTheme {
                NowPlayingMiniPlayer(state().copy(playIntent = false, failure = PlaybackFailure.SourceUnavailable(ITEM)), {}, {})
            }
        }

        val failure = onNodeWithText("Could not reach it. Tap play to try again.", useUnmergedTree = true)
        assertEquals(LiveRegionMode.Polite, failure.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
        onNodeWithContentDescription("Play $TITLE").assertExists()
    }

    @Test
    fun thePlayerScreenExposesRepeatVolumeAndPlaybackForNarratedContent() = runComposeUiTest {
        val actions = PlayerActions()
        showPlayer(state(), actions)

        onNodeWithText("Repeat playback").performScrollTo().assertIsOff().performClick()
        assertEquals(true, actions.looping)
        onNodeWithContentDescription("Playback volume").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.3f) }
        assertEquals(0.3f, actions.volume)
        onNodeWithContentDescription("Pause $TITLE").performScrollTo().performClick()
        assertEquals(1, actions.playbackClicks)
        assertEquals(0, actions.detailsOpened)
    }

    @Test
    fun allSettingsRemainReachableWithLargeTextInAShortPlayer() = runComposeUiTest {
        val actions = PlayerActions()
        showPlayer(state().copy(sleepTimerRemainingMs = 900_000L), actions, shortViewport = true)

        onNodeWithContentDescription("Pause $TITLE").performScrollTo().assertIsDisplayed()
        onNodeWithText("View details").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, actions.detailsOpened)
        onNodeWithContentDescription("Playback volume").performScrollTo().assertIsDisplayed()
        onNodeWithText("Repeat playback").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(true, actions.looping)
        onNodeWithText("60 min").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(3_600_000L, actions.timerStartedMs)
        onNodeWithText("Cancel timer").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, actions.timerCancellations)
        onNodeWithContentDescription("Back").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, actions.closed)
    }

    @Test
    fun aRestoredPlayerWithoutAnItemCanGoBackAndCancelAnExistingTimer() = runComposeUiTest {
        val actions = PlayerActions()
        showPlayer(NowPlayingState(sleepTimerRemainingMs = 60_000L), actions)

        onNodeWithText("Nothing is playing. Choose a sound or story to start.").assertIsDisplayed()
        onNodeWithText("View details").assertDoesNotExist()
        onNodeWithContentDescription("Playback volume").assertDoesNotExist()
        onNodeWithText("Cancel timer").performScrollTo().performClick()
        assertEquals(1, actions.timerCancellations)
        onNodeWithContentDescription("Back").performScrollTo().performClick()
        assertEquals(1, actions.closed)
    }

    /** The timer is what a sleep app is opened for at night: it comes right after the transport. */
    @Test
    fun theTimerComesBeforeVolumeAndARunningTimerSitsBesideItsCancel() = runComposeUiTest {
        showPlayer(state().copy(sleepTimerRemainingMs = 900_000L), PlayerActions())

        val timer = onNodeWithText("Sleep timer").fetchSemanticsNode().boundsInRoot
        val volume = onNodeWithContentDescription("Playback volume").fetchSemanticsNode().boundsInRoot
        assertTrue(timer.top < volume.top, "timer at ${timer.top}, volume at ${volume.top}")
        onNodeWithText("Stops in 15:00").assertIsDisplayed()
        onNodeWithText("Cancel timer").assertIsDisplayed()
    }

    /** The timer is the session's: it can be set before a sound or story is chosen. */
    @Test
    fun aTimerCanBeSetBeforeAnythingPlays() = runComposeUiTest {
        val actions = PlayerActions()
        showPlayer(NowPlayingState(), actions)

        onNodeWithText("15 min").performScrollTo().assertIsEnabled().performClick()
        assertEquals(900_000L, actions.timerStartedMs)
        assertEquals(0, actions.playbackClicks)
    }

    private fun ComposeUiTest.showPlayer(
        state: NowPlayingState,
        actions: PlayerActions,
        shortViewport: Boolean = false,
    ) {
        setContent {
            SleepRelaxTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, if (shortViewport) 2f else density.fontScale),
                ) {
                    Box(if (shortViewport) Modifier.size(width = 320.dp, height = 240.dp) else Modifier) {
                        NowPlayingScreen(
                            state = state,
                            onPlayPause = { actions.playbackClicks++ },
                            onVolumeChange = { actions.volume = it },
                            onLoopingChange = { actions.looping = it },
                            onTimerStart = { actions.timerStartedMs = it },
                            onTimerCancel = { actions.timerCancellations++ },
                            onBack = { actions.closed++ },
                            onOpenDetails = { actions.detailsOpened++ },
                        )
                    }
                }
            }
        }
    }

    private fun state() = NowPlayingState(
        itemId = ITEM,
        title = TITLE,
        playIntent = true,
        isLooping = false,
    )

    private class PlayerActions {
        var playbackClicks = 0
        var volume: Float? = null
        var looping: Boolean? = null
        var timerStartedMs: Long? = null
        var timerCancellations = 0
        var closed = 0
        var detailsOpened = 0
    }

    private companion object {
        // A narrated item from an arbitrary provider, without importing a sound or story feature.
        val ITEM = PlaybackItemId("narration", "bedtime")
        const val TITLE = "A quiet bedtime story"
    }
}
