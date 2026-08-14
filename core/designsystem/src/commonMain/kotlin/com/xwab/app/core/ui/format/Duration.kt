package com.xwab.app.core.ui.format

/**
 * A running time as `m:ss`, for any content a screen lists.
 *
 * It lives in the design system rather than beside a catalog because it is presentation, not data:
 * `core:sound:catalog` and `core:story:catalog` describe different content and never see each
 * other, so a formatter owned by either one leaves the other reimplementing it. That is exactly
 * what happened — the story list carried a four-line copy with a comment explaining that it could
 * not reach the sound catalog's version.
 *
 * A negative total is clamped rather than rejected: a duration that should never have been negative
 * is a catalog problem, and both models already refuse one at construction. Drawing `0:00` beats
 * crashing a list over it.
 */
fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
