package com.xwab.app.feature.story

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** Stories own these commands; no sound screen or feature-to-feature dependency is needed. */
@OptIn(ExperimentalTestApi::class)
class StoriesScreenTest {
    @Test
    fun theTimerCanBeStartedDirectlyFromStories() = runComposeUiTest {
        var durationMs: Long? = null
        show(
            StoriesState(stories = listOf(story("bedtime"))),
            onTimerStart = { durationMs = it },
        )

        onNodeWithText("Sleep timer").assertExists()
        onNodeWithText("Off").assertExists()
        onNodeWithText("15 min").performScrollTo().performClick()

        assertEquals(15 * 60_000L, durationMs)
        onNodeWithText("Cancel timer").assertDoesNotExist()
    }

    @Test
    fun anExistingTimerCanBeChangedOrCancelledFromStories() = runComposeUiTest {
        var durationMs: Long? = null
        var cancellations = 0
        show(
            StoriesState(stories = listOf(story("bedtime")), sleepTimerRemainingMs = 90_000L),
            onTimerStart = { durationMs = it },
            onTimerCancel = { cancellations++ },
        )

        onNodeWithText("Stops in 1:30").assertExists()
        onNodeWithText("30 min").performScrollTo().performClick()
        onNodeWithText("Cancel timer").performScrollTo().performClick()

        assertEquals(30 * 60_000L, durationMs)
        assertEquals(1, cancellations)
    }

    @Test
    fun anEmptyCatalogDisablesPresetsButNeverDisablesCancellation() = runComposeUiTest {
        var cancellations = 0
        show(
            StoriesState(sleepTimerRemainingMs = 60_000L),
            onTimerCancel = { cancellations++ },
        )

        onNodeWithText("15 min").assertIsNotEnabled()
        onNodeWithText("Cancel timer").assertIsEnabled().performScrollTo().performClick()

        assertEquals(1, cancellations)
    }

    private fun ComposeUiTest.show(
        state: StoriesState,
        onTimerStart: (Long) -> Unit = {},
        onTimerCancel: () -> Unit = {},
    ) {
        setContent {
            SleepRelaxTheme {
                StoriesScreen(
                    state = state,
                    onPlaybackClick = {},
                    onTimerStart = onTimerStart,
                    onTimerCancel = onTimerCancel,
                )
            }
        }
    }
}
