package com.xwab.app.feature.nowplaying

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.nowplaying.shell.NowPlayingBarContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The bar must work for every content kind, without depending on its catalog or detail screen. */
@OptIn(ExperimentalTestApi::class)
class NowPlayingBarTest {
    @Test
    fun theBarShowsItsTimerAndSeparatesOpeningFromPlayback() = runComposeUiTest {
        var opened = 0
        var playbackClicks = 0
        setContent {
            SleepRelaxTheme {
                NowPlayingBarContent(
                    state(),
                    onPlayPauseClick = { playbackClicks++ },
                    onOpenClick = { opened++ },
                    onTimerCancel = {},
                    sleepTimerRemainingMs = 900_000L,
                )
            }
        }

        onNodeWithText("Stops in 15 min").assertIsDisplayed()
        assertEquals(
            "Open details and sleep timer",
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
        val remainingMs = mutableStateOf<Long?>(900_000L)
        setContent {
            SleepRelaxTheme { NowPlayingBarContent(state(), {}, {}, {}, sleepTimerRemainingMs = remainingMs.value) }
        }

        val timer = onNodeWithText("Stops in 15 min", useUnmergedTree = true)
        timer.assertIsDisplayed()
        assertNull(timer.fetchSemanticsNode().config.getOrNull(SemanticsProperties.LiveRegion))

        runOnIdle { remainingMs.value = 840_000L }
        onNodeWithText("Stops in 14 min").assertIsDisplayed()
        runOnIdle { remainingMs.value = null }
        onNodeWithText("Stops in 14 min").assertDoesNotExist()
        onNodeWithText("Playing · Timer off").assertIsDisplayed()
    }

    @Test
    fun preparingContentCanBePausedWithoutClaimingThatItIsAlreadyPlaying() = runComposeUiTest {
        var pauses = 0
        setContent {
            SleepRelaxTheme { NowPlayingBarContent(state().copy(isPreparing = true), { pauses++ }, {}, {}) }
        }

        onNodeWithText("Loading…", useUnmergedTree = true).assertExists()
        onNodeWithText("Open details and sleep timer", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Playing · Timer off").assertDoesNotExist()
        onNodeWithContentDescription("Pause $TITLE").performClick()
        assertEquals(1, pauses)
    }

    /**
     * With nothing requested there is no item screen to open and no timer anywhere else to cancel,
     * so the bar is not a link and offers the cancel itself.
     */
    @Test
    fun aTimerWithNothingRequestedIsCancelledFromTheBarItself() = runComposeUiTest {
        var opened = 0
        var cancellations = 0
        setContent {
            SleepRelaxTheme {
                NowPlayingBarContent(
                    NowPlayingState(),
                    onPlayPauseClick = {},
                    onOpenClick = { opened++ },
                    onTimerCancel = { cancellations++ },
                    sleepTimerRemainingMs = 60_000L,
                )
            }
        }

        onNodeWithText("Stops in 1 min").assertIsDisplayed()
        onNodeWithContentDescription("Play", substring = true).assertDoesNotExist()
        assertNull(onNodeWithText("Sleep timer").fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick))
        onNodeWithText("Cancel timer").performClick()
        assertEquals(1, cancellations)
        assertEquals(0, opened)
    }

    @Test
    fun aPlaybackFailureIsAnnouncedAndOffersTheSameItemsRetry() = runComposeUiTest {
        setContent {
            SleepRelaxTheme {
                NowPlayingBarContent(
                    state().copy(playIntent = false, failure = PlaybackFailure.SourceUnavailable(ITEM)),
                    {}, {}, {},
                )
            }
        }

        val failure = onNodeWithText("Could not reach it. Tap play to try again.", useUnmergedTree = true)
        assertEquals(LiveRegionMode.Polite, failure.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
        onNodeWithContentDescription("Play $TITLE").assertExists()
    }

    private fun state() = NowPlayingState(itemId = ITEM, title = TITLE, playIntent = true)

    private companion object {
        // A narrated item from an arbitrary provider, without importing a sound or story feature.
        val ITEM = PlaybackItemId("narration", "bedtime")
        const val TITLE = "A quiet bedtime story"
    }
}
