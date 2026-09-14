package com.xwab.app.feature.sound

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import kotlin.test.Test

/**
 * What this screen says while a source is being resolved.
 *
 * [SoundViewModelTest] covers the state working `isPreparing` out; this covers the screen drawing
 * something with it. The two were worth separating here because for a while it did neither — the
 * field was computed and then read by nothing, so a slow source looked like a button that had
 * missed the tap, and no test could tell.
 *
 * In the iOS source set because the screen is common code — what the simulator draws is the same
 * composable Android draws — and because CI already runs the simulator tests.
 *
 * That reasoning has one hole, and it has cost once. The code is shared; the *host's* constraints
 * are not. A lazy list key that boxes a value class is rejected by Android's `Bundle` and accepted
 * by a simulator, so these tests watched `BrowseScreen` crash the first screen of the app without
 * seeing anything wrong. `checkArchitecture` now reports that particular shape at build time,
 * which is the cheap half of the answer.
 *
 * The other half was tried and set down: an Android host test under Robolectric, in a closed PR
 * kept for what it established rather than for what it shipped. Two of the three unknowns are
 * settled — the AGP 9.3 KMP DSL accepts `withHostTest { isIncludeAndroidResources = true }`, and
 * Robolectric runs with this Compose version once `androidx.activity.ComponentActivity` is
 * declared in a host-test manifest. What stopped it was the third: Compose Multiplatform resources
 * did not resolve from assets under Robolectric, so the screen composed nothing and the test
 * failed on its own assertion.
 *
 * Worth picking up again when a second host-specific bug gets past these tests — not before. The
 * first one is guarded by a rule now, and an Android harness costs a second set of screen tests to
 * keep.
 */
@OptIn(ExperimentalTestApi::class)
class SoundScreenTest {
    @Test
    fun aSoundThatIsWantedButNotAudibleYetSaysSo() = runComposeUiTest {
        show(SoundState(track = TRACK, playIntent = true, isPreparing = true))

        // The title first: a screen that never drew and a message that never appeared look the
        // same to the assertion below. `assertExists` rather than `assertIsDisplayed`: what is
        // checked is that the line was composed, not that it sat inside a given window.
        onNodeWithText(TRACK_NAME).assertExists()
        onNodeWithText(PREPARING).assertExists()
    }

    @Test
    fun aSoundAlreadyPlayingSaysNothingAboutPreparing() = runComposeUiTest {
        show(SoundState(track = TRACK, playIntent = true))

        onNodeWithText(TRACK_NAME).assertExists()
        onNodeWithText(PREPARING).assertDoesNotExist()
    }

    /** The wording the lists use for the same failure, so one broken sound reads one way. */
    @Test
    fun aSoundThatCouldNotBeReachedSaysWhy() = runComposeUiTest {
        show(SoundState(track = TRACK, error = SoundError.SoundUnavailable))

        onNodeWithText(UNAVAILABLE).assertExists()
    }

    /**
     * A control that cannot act must not look like it can.
     *
     * The heart used to be drawn live over a sound the catalog no longer held and do nothing when
     * tapped, because the refusal lived in the ViewModel and the drawing did not know. The play
     * button was worse: nothing refused it, so it asked the session for a sound nothing could
     * resolve.
     */
    @Test
    fun aSoundTheCatalogDoesNotHoldOffersNothingToTap() = runComposeUiTest {
        show(SoundState(track = null, error = SoundError.SoundNotFound))

        onNodeWithText(NOT_FOUND).assertExists()
        onNodeWithContentDescription(PLAY).assertIsNotEnabled()
        onNodeWithContentDescription(ADD_FAVORITE).assertIsNotEnabled()
    }

    /**
     * The volume label formats a number into a resource, and the sign it ends in is the part that
     * went wrong: the string escaped its percent the way an Android XML string would, and Compose
     * Multiplatform passed both characters through, so a device read "100%%" from the first commit
     * onward. Spelled out here rather than built from the same resource, so the test disagrees with
     * the string instead of agreeing with whatever it happens to say.
     */
    @Test
    fun theVolumeLabelEndsInOnePercentSign() = runComposeUiTest {
        show(SoundState(track = TRACK, volume = 1.0f))

        onNodeWithText(FULL_VOLUME).assertExists()
    }

    private fun ComposeUiTest.show(state: SoundState) {
        setContent {
            SleepRelaxTheme {
                SoundScreen(
                    state = state,
                    onBack = {},
                    onFavoriteClick = {},
                    onPlaybackClick = {},
                    onLoopingChange = {},
                    onVolumeChange = {},
                    onTimerStart = {},
                    onTimerCancel = {},
                )
            }
        }
    }

    private companion object {
        const val TRACK_NAME = "Ontario Waves"
        val TRACK = Track(
            id = TrackId("calm-waves"),
            name = TRACK_NAME,
            categoryId = CategoryId("ocean"),
            durationSeconds = 286,
        )

        /** The words a listener reads, spelled out rather than read back from the same resource. */
        const val PREPARING = "Loading\u2026"
        const val UNAVAILABLE = "Could not reach this sound. Tap play to try again."
        const val NOT_FOUND = "This sound is no longer in the catalog"
        const val FULL_VOLUME = "100%"

        /** What a screen reader announces for the two controls, from the design system. */
        const val PLAY = "Play"
        const val ADD_FAVORITE = "Add to favorites"
    }
}
