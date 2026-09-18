package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/**
 * The frame every screen in this app sits in: the gradient, and content held to a readable width in
 * the middle of whatever window it is given.
 *
 * It was spelled out five times — once per screen, as
 * `SleepRelaxBackground { … .widthIn(max = contentMaxWidth).fillMaxSize().align(Center) }` — and it
 * is the kind of repetition that drifts silently, because no screen is ever looked at beside
 * another. It already had: see [screenContentPadding].
 *
 * A [BoxScope] rather than a column, because what a screen puts inside is its own business: three of
 * them are a lazy list, one is a scrolling column and one is a plain one. This owns where the
 * content sits, not what it is.
 */
@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    SleepRelaxBackground(modifier) {
        Box(
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center),
            content = content,
        )
    }
}

/**
 * The inset from a screen's frame to the screen's own content.
 *
 * Returned as [PaddingValues] rather than applied by [ScreenContainer] because where it belongs
 * depends on what the screen is: a lazy list has to take it as `contentPadding`, or the first and
 * last rows clip against the edge instead of scrolling past it, while a plain column takes it as a
 * modifier. Both are the same inset, which is the part worth sharing.
 *
 * The five screens disagreed on the bottom of it: three used the vertical screen padding and two
 * used `spacingHuge`, a third of a step smaller, with nothing anywhere saying why — so scrolling
 * the stories tab and then the favorites tab stopped in two different places. The named token
 * wins, because `paddingScreenVertical` is the one that says what it is for.
 */
@Composable
fun screenContentPadding(): PaddingValues = PaddingValues(
    horizontal = SleepRelaxTheme.dimens.paddingScreenHorizontal,
    vertical = SleepRelaxTheme.dimens.paddingScreenVertical,
)
