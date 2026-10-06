package com.xwab.app.core.playback.timer

import com.xwab.app.core.playback.port.SleepTimerState
import com.xwab.app.core.playback.store.remainingDurationUntil
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The monotonic millisecond clock sleep-timer deadlines are expressed in.
 *
 * A deadline only means something against the clock that produced it, so whatever computes one and
 * the [SleepTimerTicker] counting down to it read the same clock: each platform's graph binds it
 * once for both.
 */
internal fun interface SleepTimerClock {
    fun nowMs(): Long
}

/**
 * Schedules the next countdown tick on the platform's main thread.
 *
 * Only the scheduling primitive differs per platform (a main-looper `Handler` on
 * Android, a main-scope coroutine on iOS); the countdown policy itself is shared
 * and lives in [SleepTimerTicker].
 *
 * Scheduling replaces the pending tick, so one scheduler serves one owner. The platform graphs bind
 * it unscoped, and every injection gets its own; scoping it would let two owners cancel each
 * other's ticks.
 */
internal interface TickScheduler {

    /** Run [action] after [delayMs], replacing any previously scheduled tick. */
    fun schedule(delayMs: Long, action: () -> Unit)

    /** Drop a pending tick, if any. */
    fun cancel()

    /** Permanently stop scheduling. Defaults to a plain [cancel]. */
    fun release() = cancel()
}

/**
 * The sleep-timer countdown shared by both platforms: it owns the published
 * [SleepTimerState] and re-arms itself once per second until the deadline passes.
 *
 * Tracking a deadline rather than decrementing a counter keeps the countdown
 * accurate when a tick is delayed (e.g. in the background), and lets Android adopt
 * the deadline its PlaybackService owns. [clock] and every deadline passed to
 * [applyDeadline] must therefore come from the *same* monotonic clock.
 *
 * Must be used from the platform's main thread.
 *
 * @param onFadeVolume receives the player volume over the last [SLEEP_TIMER_FADE_MS], and full
 *   volume again once the timer expires, is cancelled or is restarted — after [onExpired] has
 *   stopped playback, so the restored level is never heard. A no-op where this ticker only
 *   mirrors a timer another component plays out (Android's PlaybackService fades itself).
 */
@AssistedInject
internal class SleepTimerTicker(
    private val clock: SleepTimerClock,
    private val scheduler: TickScheduler,
    @Assisted private val onExpired: () -> Unit,
    @Assisted private val onFadeVolume: (Float) -> Unit,
) {
    /** The callbacks are the owner's; the clock and the scheduler come from the platform graph. */
    @AssistedFactory
    fun interface Factory {
        fun create(onExpired: () -> Unit, onFadeVolume: (Float) -> Unit): SleepTimerTicker
    }

    private val mutableState = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = mutableState.asStateFlow()

    private var deadlineMs: Long? = null
    private var appliedVolume = FULL_VOLUME

    /** Adopt [newDeadlineMs] (null clears the timer) and restart the countdown. */
    fun applyDeadline(newDeadlineMs: Long?) {
        deadlineMs = newDeadlineMs
        scheduler.cancel()
        publishRemainingTime()
    }

    /** Clear an active timer without reporting expiry. */
    fun clear() = applyDeadline(null)

    /** Permanently tear down; no further ticks or expiry callbacks are delivered. */
    fun release() {
        deadlineMs = null
        scheduler.release()
        mutableState.value = SleepTimerState()
    }

    private fun publishRemainingTime() {
        val deadline = deadlineMs ?: run {
            mutableState.value = SleepTimerState()
            applyVolume(FULL_VOLUME)
            return
        }

        val remainingMs = remainingDurationUntil(deadline, clock.nowMs())
        if (remainingMs == null) {
            deadlineMs = null
            mutableState.value = SleepTimerState()
            onExpired()
            applyVolume(FULL_VOLUME)
            return
        }

        mutableState.value = SleepTimerState(remainingMs)
        applyVolume(sleepTimerFadeVolume(remainingMs))
        scheduler.schedule(sleepTimerTickDelay(remainingMs, UPDATE_INTERVAL_MS)) { publishRemainingTime() }
    }

    private fun applyVolume(volume: Float) {
        if (volume == appliedVolume) return
        appliedVolume = volume
        onFadeVolume(volume)
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 1_000L
        const val FULL_VOLUME = 1.0f
    }
}
