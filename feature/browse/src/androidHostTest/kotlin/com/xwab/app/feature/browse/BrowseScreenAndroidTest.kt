package com.xwab.app.feature.browse

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one check the simulator tests structurally cannot make.
 *
 * This grid crashed the first screen the app opens by handing a `CategoryId` to a lazy key. A key
 * is held as `Any`, so the value class boxed, and on Android the saveable state holder behind a
 * navigation entry writes those keys into a `Bundle`, which cannot hold one. The screen tests in
 * `iosTest` accept the same key, because a simulator has no `Bundle` — the constraint belongs to
 * the host, not to the shared code.
 *
 * So this runs the composable on Android. `checkArchitecture` also reports the shape that went
 * wrong, and the two catch it from opposite ends: the rule at build time over spelling it
 * recognises, this at run time over whatever a key actually turns out to be.
 *
 * Pinned to an SDK Robolectric ships a sandbox for, which trails the one this app compiles
 * against.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowseScreenAndroidTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theGridDrawsCategoriesUnderKeysAndroidCanStore() {
        compose.setContent {
            SleepRelaxTheme {
                BrowseScreen(
                    state = BrowseState(listOf(RAIN, OCEAN)),
                    onCategoryClick = {},
                )
            }
        }

        // Measuring the grid is what composes an item and hands its key to the state holder, so
        // reading a row is also what proves the key was storable.
        compose.onNodeWithText(RAIN_NAME).assertExists()
        compose.onNodeWithText(OCEAN_NAME).assertExists()
    }

    private companion object {
        const val RAIN_NAME = "Rain"
        const val OCEAN_NAME = "Ocean"
        val RAIN = Category(CategoryId("rain"), RAIN_NAME, "Gentle raindrops", "\u2602", 2)
        val OCEAN = Category(CategoryId("ocean"), OCEAN_NAME, "Slow waves", "\u301c", 3)
    }
}
