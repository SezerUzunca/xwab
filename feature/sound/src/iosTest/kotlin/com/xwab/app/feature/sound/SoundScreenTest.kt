package com.xwab.app.feature.sound

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
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
        const val UNAVAILABLE = "Could not reach this sound. Tap to try again."
    }
}
