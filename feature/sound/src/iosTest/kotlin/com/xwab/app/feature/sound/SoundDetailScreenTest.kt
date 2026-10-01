package com.xwab.app.feature.sound

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.sound.domain.SoundFavoriteReadStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The sound's own actions, and the session's sleep timer in a card below them. */
@OptIn(ExperimentalTestApi::class)
class SoundDetailScreenTest {
    @Test
    fun preparingAndFailureAreVisibleBesidePlayback() = runComposeUiTest {
        show(SoundState(track = TRACK, playIntent = true, isPreparing = true))
        onNodeWithText(TRACK_NAME).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("\u266A").assertDoesNotExist()
        onNodeWithText("Loading\u2026").assertExists()
        onNodeWithContentDescription("Pause $TRACK_NAME").assertIsEnabled()
    }

    @Test
    fun aFailedSoundSaysHowToRetry() = runComposeUiTest {
        show(SoundState(track = TRACK, error = SoundError.SoundUnavailable))
        onNodeWithText("Could not reach this sound. Tap play to try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun missingContentCannotBePlayedOrFavorited() = runComposeUiTest {
        show(SoundState(error = SoundError.SoundNotFound, favoriteReadStatus = SoundFavoriteReadStatus.Available))
        onNodeWithText("This sound is no longer in the catalog").assertExists()
        onNodeWithContentDescription("Play").assertIsNotEnabled()
        onNodeWithContentDescription("Favorite").assertIsNotEnabled().assertIsOff()
        onNodeWithText("Public Domain", substring = true).assertDoesNotExist()
        onNodeWithText("Internet required").assertDoesNotExist()
    }

    /**
     * The item's own actions come first; the session's timer sits in a card below them. There is
     * no repeat or volume control: a sound always loops, and the phone's keys set loudness.
     */
    @Test
    fun theTimerSitsBelowTheItemsOwnActions() = runComposeUiTest {
        val actions = Actions()
        show(SoundState(track = TRACK), actions)
        onNodeWithContentDescription("Play $TRACK_NAME").performClick()
        assertEquals(1, actions.playbackClicks)
        onNodeWithContentDescription("Minutes").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertNull(actions.timerStartedMs, "selecting a duration must not start the timer")
        onNodeWithText("Start timer").performScrollTo().performClick()
        assertEquals(900_000L, actions.timerStartedMs)
        assertEquals(1, actions.playbackClicks, "the timer must not touch playback")
        onNodeWithText("Repeat playback").assertDoesNotExist()
        onNodeWithText("Sound volume").assertDoesNotExist()
        // One pass of a sound that repeats: a bare "4:46" would read as "stops after 4:46".
        onNodeWithText("4:46 loop • Public Domain").assertExists()
        onNodeWithText("Internet required").assertExists()
    }

    @Test
    fun aRunningTimerShowsWhatIsLeftBesideItsCancel() = runComposeUiTest {
        val actions = Actions()
        show(SoundState(track = TRACK), actions, sleepTimerRemainingMs = 90_000L)
        onNodeWithText("Stops in 2 min").performScrollTo().assertExists()
        onNodeWithText("Cancel timer").performScrollTo().performClick()
        assertEquals(1, actions.timerCancellations)
    }

    @Test
    fun changingTheWheelOnlyRestartsTheTimerWhenConfirmed() = runComposeUiTest {
        val actions = Actions()
        show(SoundState(track = TRACK), actions, sleepTimerRemainingMs = 90_000L)
        onNodeWithContentDescription("Minutes").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        assertNull(actions.timerStartedMs)
        onNodeWithText("Stops in 2 min").assertExists()
        onNodeWithText("Restart timer").performScrollTo().performClick()
        assertEquals(5_400_000L, actions.timerStartedMs)
        assertEquals(0, actions.playbackClicks)
    }

    @Test
    fun onlyTheOriginalMinuteDurationsCanBeSelected() = runComposeUiTest {
        val actions = Actions()
        show(SoundState(track = TRACK), actions)
        onNodeWithContentDescription("Hours").assertDoesNotExist()
        onNodeWithContentDescription("Seconds").assertDoesNotExist()
        listOf(15, 30, 60, 90).forEachIndexed { index, minutes ->
            val previousTimer = actions.timerStartedMs
            onNodeWithContentDescription("Minutes").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(index.toFloat()) }
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "$minutes min"))
            assertEquals(previousTimer, actions.timerStartedMs, "scrolling must not change the timer")
            onNodeWithText("Start timer").performScrollTo().performClick()
            assertEquals(minutes * 60_000L, actions.timerStartedMs)
        }
    }

    @Test
    fun verifiedCachedContentIsLabeledAvailableOffline() = runComposeUiTest {
        show(SoundState(track = TRACK, availableOffline = true))
        onNodeWithText("Available offline").assertExists()
        onNodeWithText("Internet required").assertDoesNotExist()
    }

    @Test
    fun pendingFavoritesDoNotPreventPlayback() = runComposeUiTest {
        show(SoundState(track = TRACK))
        onNodeWithContentDescription("Play $TRACK_NAME").assertIsEnabled()
        onNodeWithContentDescription("Loading favorites").assertIsNotEnabled()
    }

    @Test
    fun failedFavoritesKeepPreviousMembershipAndExplainTheFailure() = runComposeUiTest {
        show(SoundState(track = TRACK, isFavorite = true, favoriteReadStatus = SoundFavoriteReadStatus.Unavailable))
        onNodeWithContentDescription("Favorite $TRACK_NAME").assertIsNotEnabled().assertIsOn()
        onNodeWithText("Favorites are temporarily unavailable. Retrying\u2026").assertExists()
    }

    private fun ComposeUiTest.show(
        state: SoundState,
        actions: Actions = Actions(),
        sleepTimerRemainingMs: Long? = null,
    ) {
        setContent {
            SleepRelaxTheme {
                SoundDetailScreen(
                    state,
                    onBack = {},
                    onFavoriteClick = {},
                    onPlaybackClick = { actions.playbackClicks++ },
                    onTimerStart = { actions.timerStartedMs = it },
                    onTimerCancel = { actions.timerCancellations++ },
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                )
            }
        }
    }

    private class Actions {
        var playbackClicks = 0
        var timerStartedMs: Long? = null
        var timerCancellations = 0
    }

    private companion object {
        const val TRACK_NAME = "Ontario Waves"
        val TRACK = Track(TrackId("calm-waves"), TRACK_NAME, CategoryId("ocean"), 286)
    }
}
