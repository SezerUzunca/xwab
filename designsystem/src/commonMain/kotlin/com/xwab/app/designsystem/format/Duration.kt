package com.xwab.app.designsystem.format

/**
 * A running time as `m:ss`, for any content a screen lists.
 *
 * It lives in the design system rather than beside a catalog because it is presentation, not data:
 * `core:sound` and `core:story` describe different content and never see each
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

/**
 * A countdown as `m:ss`, for a deadline a screen is showing while it runs down.
 *
 * Rounded **up**, not down: with 200 ms left this answers `0:01`, not `0:00`. A countdown that
 * reads zero while the thing it counts is still running looks stuck, and the last second is the
 * one a listener is most likely to be watching.
 *
 * Beside [formatDuration] for the reason that one gives — it is presentation, and a formatter
 * owned by one caller leaves the next reimplementing it. This one lived as a private function at
 * the bottom of a screen file, untested, with the rounding above nowhere stated.
 */
fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs.coerceAtLeast(0L) + 999L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
