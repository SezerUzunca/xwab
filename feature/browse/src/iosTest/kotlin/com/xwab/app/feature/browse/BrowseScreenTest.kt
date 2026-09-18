package com.xwab.app.feature.browse

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.designsystem.theme.darkColors
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BrowseScreenTest {
    @Test
    fun categoryCountsUseTheReadableAccentWithoutReducingItsOpacity() = runComposeUiTest {
        setContent {
            SleepRelaxTheme {
                BrowseScreen(
                    BrowseState(listOf(Category(CategoryId("rain"), "Rain", "Gentle rain", "☂", 2))),
                    onCategoryClick = {},
                )
            }
        }

        // Checking only the theme could not catch the old 55% alpha at this call site. Assert the
        // actual text style as well; ContrastTest checks that colour against the card gradient.
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("2 tracks", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(darkColors.accent, layouts.single().layoutInput.style.color)
    }
}
