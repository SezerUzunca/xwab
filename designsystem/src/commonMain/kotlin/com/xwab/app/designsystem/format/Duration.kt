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
 * What is left on a countdown in whole minutes, for every place the sleep timer is shown.
 *
 * Minutes rather than `m:ss`: at night a number changing every second is one more thing moving in
 * the dark, and nobody setting a sleep timer needs the seconds. One function, so the bar and the
 * timer card never show the same timer two ways.
 *
 * Rounded **up**, not down: with 20 seconds left this answers 1, not 0. A countdown that reads zero
 * while the sound is still playing looks stuck.
 */
fun remainingWholeMinutes(remainingMs: Long): Long {
    val safeMs = remainingMs.coerceAtLeast(0L)
    return (safeMs + MINUTE_MS - 1L) / MINUTE_MS
}

private const val MINUTE_MS = 60_000L
