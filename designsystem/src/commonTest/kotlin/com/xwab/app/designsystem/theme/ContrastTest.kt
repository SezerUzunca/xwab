package com.xwab.app.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The palette is checked rather than described.
 *
 * Secondary text spent every release so far at roughly 4.3:1 against the surfaces it is drawn on —
 * close enough to look deliberate and low enough to fail WCAG AA for text its size. Nothing said
 * so, because a colour is a number and a number cannot be reviewed by looking at it. This is the
 * one rule that can: whatever the palette becomes, text has to stay readable on what it sits on.
 *
 * Only the dark palette exists, so these are the whole of the backgrounds text can end up over.
 */
class ContrastTest {

    @Test
    fun everyTextColorIsReadableOnEverySurfaceItIsDrawnOn() {
        val failures = textColors.flatMap { (textName, text) ->
            backgrounds.mapNotNull { (surfaceName, surface) ->
                val ratio = contrastRatio(text.over(surface), surface)
                "$textName on $surfaceName is ${ratio.rounded()}:1".takeIf { ratio < AA_NORMAL_TEXT }
            }
        }
        assertTrue(
            failures.isEmpty(),
            "WCAG AA asks for $AA_NORMAL_TEXT:1 for text this size: ${failures.joinToString()}",
        )
    }

    /**
     * The glass card is the case that actually failed, and the one a reviewer is least likely to
     * check: it lightens the background under it, so text on a card has *less* contrast than the
     * same text on the gradient it sits on.
     */
    @Test
    fun theGlassCardIsTheHardestBackgroundSecondaryTextHas() {
        val onCard = contrastRatio(
            darkColors.textSecondary.over(glassCardOver(darkColors.backgroundTop)),
            glassCardOver(darkColors.backgroundTop),
        )
        val onGradient = contrastRatio(
            darkColors.textSecondary.over(darkColors.backgroundTop),
            darkColors.backgroundTop,
        )
        assertTrue(
            onCard < onGradient,
            "the glass card should be the tighter case, but reads ${onCard.rounded()}:1 " +
                "against ${onGradient.rounded()}:1 on the bare gradient",
        )
    }

    private val textColors = listOf(
        "primary text" to darkColors.textPrimary,
        "secondary text" to darkColors.textSecondary,
        "error text" to darkColors.error,
        // A text colour as much as a tint: the selected tab label and every text button use it.
        "accent text" to darkColors.accent,
    )

    private val backgrounds = listOf(
        "the top of the gradient" to darkColors.backgroundTop,
        "the bottom of the gradient" to darkColors.backgroundBottom,
        "a surface" to darkColors.surface,
        "a glass card on the gradient top" to glassCardOver(darkColors.backgroundTop),
        "a glass card on the gradient bottom" to glassCardOver(darkColors.backgroundBottom),
    )
}

/** What `Modifier.glassCard()` leaves behind the text it holds: 8% white over whatever it sits on. */
private fun glassCardOver(background: Color): Color = darkColors.glassWhite.over(background)

/**
 * This colour composited onto an opaque [background].
 *
 * Done in sRGB rather than linear space because that is where the renderer blends it, and
 * therefore what a listener's eye is actually given.
 */
private fun Color.over(background: Color): Color = Color(
    red = red * alpha + background.red * (1f - alpha),
    green = green * alpha + background.green * (1f - alpha),
    blue = blue * alpha + background.blue * (1f - alpha),
)

/** WCAG 2.1 contrast between two **opaque** colours, from 1:1 to 21:1. */
private fun contrastRatio(foreground: Color, background: Color): Double {
    val lighter = maxOf(foreground.relativeLuminance(), background.relativeLuminance())
    val darker = minOf(foreground.relativeLuminance(), background.relativeLuminance())
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Color.relativeLuminance(): Double =
    0.2126 * red.toLinear() + 0.7152 * green.toLinear() + 0.0722 * blue.toLinear()

private fun Float.toLinear(): Double = toDouble().let { channel ->
    if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}

private fun Double.rounded(): String {
    val tenths = (this * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

/** WCAG 2.1 success criterion 1.4.3, for text below 18pt (or 14pt bold). */
private const val AA_NORMAL_TEXT = 4.5
