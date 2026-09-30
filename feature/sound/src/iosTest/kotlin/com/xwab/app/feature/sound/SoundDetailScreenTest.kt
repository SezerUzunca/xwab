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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.sound.domain.SoundFavoriteReadStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** The sound's own actions, and the session's timer and repeat in a card below them. */
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

    /** The item's own actions come first; the session's timer and repeat sit in a card below them. */
    @Test
    fun theSessionSettingsSitBelowTheItemsOwnActions() = runComposeUiTest {
        val actions = Actions()
        show(SoundState(track = TRACK), actions)
        onNodeWithContentDescription("Play $TRACK_NAME").performClick()
        assertEquals(1, actions.playbackClicks)
        onNodeWithText("15 min").performScrollTo().performClick()
        assertEquals(900_000L, actions.timerStartedMs)
        onNodeWithText("Repeat playback").performScrollTo().assertIsOff().performClick()
        assertEquals(true, actions.looping)
        assertEquals(1, actions.playbackClicks, "settings must not touch playback")
        onNodeWithText("Sound volume").assertDoesNotExist()
        // One pass of a sound that repeats: a bare "4:46" would read as "stops after 4:46".
        onNodeWithText("4:46 loop • Public Domain").assertExists()
        onNodeWithText("Internet required").assertExists()
    }

    @Test
    fun aRunningTimerShowsWhatIsLeftBesideItsCancel() = runComposeUiTest {
        val actions = Actions()
        show(SoundState(track = TRACK, isLooping = true), actions, sleepTimerRemainingMs = 90_000L)
        onNodeWithText("Stops in 1:30").performScrollTo().assertExists()
        onNodeWithText("Repeat playback").performScrollTo().assertIsOn()
        onNodeWithText("Cancel timer").performScrollTo().performClick()
        assertEquals(1, actions.timerCancellations)
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
                    onLoopingChange = { actions.looping = it },
                    onTimerStart = { actions.timerStartedMs = it },
                    onTimerCancel = { actions.timerCancellations++ },
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                )
            }
        }
    }

    private class Actions {
        var playbackClicks = 0
        var looping: Boolean? = null
        var timerStartedMs: Long? = null
        var timerCancellations = 0
    }

    private companion object {
        const val TRACK_NAME = "Ontario Waves"
        val TRACK = Track(TrackId("calm-waves"), TRACK_NAME, CategoryId("ocean"), 286)
    }
}
