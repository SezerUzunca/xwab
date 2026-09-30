package com.xwab.app.feature.story

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class StoriesScreenTest {
    @Test
    fun rowBodyOpensDetailsWithoutChangingPlayback() = runComposeUiTest {
        var opened: StoryId? = null
        var played: StoryId? = null
        showList(onStoryClick = { opened = it }, onPlaybackClick = { played = it })
        onNodeWithText("bedtime").performClick()
        assertEquals(StoryId("bedtime"), opened)
        assertNull(played)
    }

    @Test
    fun playButtonChangesPlaybackWithoutOpeningDetails() = runComposeUiTest {
        var opened: StoryId? = null
        var played: StoryId? = null
        showList(onStoryClick = { opened = it }, onPlaybackClick = { played = it })
        onNodeWithContentDescription("Play bedtime").performClick()
        assertEquals(StoryId("bedtime"), played)
        assertNull(opened)
        onNodeWithText("Sleep timer").assertDoesNotExist()
    }

    @Test
    fun storyDetailsDescribeTheItemAndKeepPlayExplicit() = runComposeUiTest {
        var plays = 0
        var backs = 0
        var timerStartedMs: Long? = null
        setContent {
            SleepRelaxTheme {
                StoryDetailScreen(
                    StoryDetailState(story = story("bedtime")),
                    onBack = { backs++ },
                    onPlaybackClick = { plays++ },
                    onTimerStart = { timerStartedMs = it },
                    onTimerCancel = {},
                )
            }
        }
        onNodeWithText("A test story.").assertExists()
        onNodeWithText("bedtime").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("Narrated by A narrator").assertExists()
        onNodeWithText("Kate Chopin • 3:00").assertExists()
        onNodeWithText("Internet required").assertExists()
        assertEquals(0, plays)
        onNodeWithContentDescription("Play bedtime").performClick()
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, plays)
        assertEquals(1, backs)

        // The session's timer sits on the story's own screen and does not start it. A story plays
        // once, so there is no repeat control.
        onNodeWithText("30 min").performScrollTo().performClick()
        assertEquals(1_800_000L, timerStartedMs)
        onNodeWithText("Repeat playback").assertDoesNotExist()
        assertEquals(1, plays)
    }

    @Test
    fun missingStoryHasAReadableErrorAndDisabledPlayback() = runComposeUiTest {
        setContent {
            SleepRelaxTheme { StoryDetailScreen(StoryDetailState(), {}, {}, {}, {}) }
        }
        onNodeWithText("This story is no longer in the catalog")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNodeWithContentDescription("Play").assertIsNotEnabled()
    }

    private fun ComposeUiTest.showList(
        onStoryClick: (StoryId) -> Unit,
        onPlaybackClick: (StoryId) -> Unit,
    ) {
        setContent {
            SleepRelaxTheme {
                StoriesScreen(StoriesState(stories = listOf(story("bedtime"))), onPlaybackClick, onStoryClick)
            }
        }
    }
}
