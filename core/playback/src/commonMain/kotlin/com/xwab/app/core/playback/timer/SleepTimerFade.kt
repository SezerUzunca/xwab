package com.xwab.app.core.playback.timer

/**
 * How long before a sleep timer's deadline playback starts to fade out.
 *
 * A sound that stops dead is a change the sleeper can hear, and a steady background sound going
 * silent is exactly the contrast that wakes people. Fading over the last half minute lets it leave
 * without an edge.
 */
internal const val SLEEP_TIMER_FADE_MS = 30_000L

/** How often the fade lowers the volume: small enough that the fall is not heard as steps. */
internal const val SLEEP_TIMER_FADE_STEP_MS = 250L

/**
 * The player volume for a timer with [remainingMs] left: full until the fade window, then down to
 * silence at the deadline.
 *
 * Squared rather than linear. Loudness is heard on a logarithmic scale, so a linear fall in gain
 * sounds like almost nothing happening until a sudden drop at the very end; the square spreads the
 * audible change across the whole window.
 */
internal fun sleepTimerFadeVolume(remainingMs: Long): Float {
    val fraction = (remainingMs.toFloat() / SLEEP_TIMER_FADE_MS).coerceIn(0f, 1f)
    return fraction * fraction
}

/** How long to wait before the next countdown tick: once a second, or once a fade step while fading. */
internal fun sleepTimerTickDelay(remainingMs: Long, secondMs: Long): Long =
    remainingMs.coerceAtMost(if (remainingMs <= SLEEP_TIMER_FADE_MS) SLEEP_TIMER_FADE_STEP_MS else secondMs)
