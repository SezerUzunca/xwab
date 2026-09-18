package com.xwab.app.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.category.domain.CategoryFavoritesReadStatus
import kotlin.test.Test

/**
 * What a row of this list actually says, which is the half no ViewModel test reaches.
 *
 * [CategoryViewModelTest] covers the answers the state gives; this covers the screen asking for
 * them and drawing what comes back. The two were worth separating here because this list used to
 * draw its own card, with nowhere to put either line: a tap on a sound that could not be reached
 * did nothing visible at all, and nothing in the build could tell.
 *
 * In the iOS source set because the screen is common code — what the simulator draws is the same
 * composable Android draws — and because CI already runs the simulator tests.
 */
@OptIn(ExperimentalTestApi::class)
class CategoryScreenTest {
    @Test
    fun exactlyOneRowSaysItIsBeingPrepared() = runComposeUiTest {
        show(state(requestedTrackId = GENTLE_RAIN, isPreparing = true))

        // Both rows first: a list that never drew and a message that never appeared look the same
        // to an assertion that only counts messages. `assertExists` rather than `assertIsDisplayed`
        // throughout: what is being checked is that the screen composed the line, not that a test
        // window of some particular size happened to have it on screen.
        onNodeWithText(GENTLE_RAIN_NAME).assertExists()
        onNodeWithText(HEAVY_RAIN_NAME).assertExists()

        onAllNodesWithText(PREPARING).assertCountEquals(1)
    }

    @Test
    fun aSoundThatCouldNotBeReachedSaysWhy() = runComposeUiTest {
        show(
            state(
                playbackFailure = PlaybackFailure.SourceUnavailable(
                    PlaybackItemId.sound(GENTLE_RAIN.value),
                ),
            ),
        )

        onNodeWithText(GENTLE_RAIN_NAME).assertExists()
        onAllNodesWithText(UNAVAILABLE).assertCountEquals(1)
    }

    @Test
    fun anIdleListSaysNeither() = runComposeUiTest {
        show(state())

        onNodeWithText(GENTLE_RAIN_NAME).assertExists()
        onAllNodesWithText(PREPARING).assertCountEquals(0)
        onAllNodesWithText(UNAVAILABLE).assertCountEquals(0)
    }

    /**
     * A route outlives the build that wrote it, so a restored back stack can name a category the
     * catalog no longer holds. That used to draw a blank heading above "0 tracks" — a screen that
     * looks broken rather than one that answers.
     */
    @Test
    fun aCategoryTheCatalogDoesNotHoldSaysSoInsteadOfDrawingABlankOne() = runComposeUiTest {
        show(CategoryState())

        onNodeWithText(CATEGORY_NOT_FOUND).assertExists()
        onAllNodesWithText(GENTLE_RAIN_NAME).assertCountEquals(0)
    }

    @Test
    fun aPendingFavoriteReadKeepsTheRowsVisibleWithoutAnError() = runComposeUiTest {
        show(state().copy(favoritesReadStatus = CategoryFavoritesReadStatus.Pending))

        onNodeWithText(GENTLE_RAIN_NAME).assertExists()
        onNodeWithText(HEAVY_RAIN_NAME).assertExists()
        onAllNodesWithContentDescription(FAVORITES_LOADING).assertCountEquals(2)
        onNodeWithText(FAVORITES_UNAVAILABLE).assertDoesNotExist()
    }

    @Test
    fun anUnavailableFavoriteReadKeepsThePreviousHeartsAndShowsTheError() = runComposeUiTest {
        show(state().copy(
            favoriteIds = setOf(GENTLE_RAIN),
            favoritesReadStatus = CategoryFavoritesReadStatus.Unavailable,
        ))

        onNodeWithContentDescription(REMOVE_FAVORITE).assertIsNotEnabled()
        onAllNodesWithContentDescription(FAVORITES_LOADING).assertCountEquals(0)
        onNodeWithText(FAVORITES_UNAVAILABLE).assertExists()
    }

    @Test
    fun tracksRemainReachableBelowALargeHeaderInAShortViewport() = runComposeUiTest {
        val content = state().copy(
            category = state().category?.copy(name = "A long category heading that wraps"),
            favoritesReadStatus = CategoryFavoritesReadStatus.Unavailable,
        )
        show(content, shortViewport = true)

        // Unlike a fixed header above a nested list, one list can scroll even when its header
        // alone is taller than the available height (landscape, split screen or player chrome).
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText(HEAVY_RAIN_NAME))
        onNodeWithText(HEAVY_RAIN_NAME).assertIsDisplayed()

        onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    private fun ComposeUiTest.show(state: CategoryState, shortViewport: Boolean = false) {
        setContent {
            SleepRelaxTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, if (shortViewport) 2f else density.fontScale),
                ) {
                    Box(if (shortViewport) Modifier.size(width = 360.dp, height = 240.dp) else Modifier) {
                        CategoryScreen(
                            state = state,
                            onTrackClick = {},
                            onFavoriteClick = {},
                            onPlaybackClick = {},
                            onBack = {},
                        )
                    }
                }
            }
        }
    }

    private fun state(
        requestedTrackId: TrackId? = null,
        isPreparing: Boolean = false,
        playbackFailure: PlaybackFailure? = null,
    ) = CategoryState(
        category = Category(RAIN, "Rain", "Gentle raindrops", "\u2602", 2),
        tracks = listOf(
            Track(GENTLE_RAIN, GENTLE_RAIN_NAME, RAIN, durationSeconds = 12),
            Track(HEAVY_RAIN, HEAVY_RAIN_NAME, RAIN, durationSeconds = 45),
        ),
        requestedTrackId = requestedTrackId,
        playIntent = requestedTrackId != null,
        isPreparing = isPreparing,
        playbackFailure = playbackFailure,
        favoritesReadStatus = CategoryFavoritesReadStatus.Available,
    )

    private companion object {
        val RAIN = CategoryId("rain")
        val GENTLE_RAIN = TrackId("gentle-rain")
        val HEAVY_RAIN = TrackId("heavy-rain")
        const val GENTLE_RAIN_NAME = "Rain on the Window"
        const val HEAVY_RAIN_NAME = "Heavy Rain"

        /** The words a listener reads, spelled out rather than read back from the same resource. */
        const val PREPARING = "Loading\u2026"
        const val UNAVAILABLE = "Could not reach this sound. Tap play to try again."
        const val CATEGORY_NOT_FOUND = "This category is no longer in the catalog"
        const val FAVORITES_LOADING = "Loading favorites"
        const val FAVORITES_UNAVAILABLE = "Favorites are temporarily unavailable. Retrying\u2026"
        const val REMOVE_FAVORITE = "Remove from favorites"
    }
}
