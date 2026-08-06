package com.xwab.app.core.playbackengine.timer

import com.xwab.app.core.playbackengine.api.SleepTimerState
import com.xwab.app.core.playbackengine.store.remainingDurationUntil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Schedules the next countdown tick on the platform's main thread.
 *
 * Only the scheduling primitive differs per platform (a main-looper `Handler` on
 * Android, a main-scope coroutine on iOS); the countdown policy itself is shared
 * and lives in [SleepTimerTicker].
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
 * the deadline its PlaybackService owns. [nowMs] and every deadline passed to
 * [applyDeadline] must therefore come from the *same* monotonic clock.
 *
 * Must be used from the platform's main thread.
 */
internal class SleepTimerTicker(
    private val nowMs: () -> Long,
    private val scheduler: TickScheduler,
    private val onExpired: () -> Unit,
) {
    private val mutableState = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = mutableState.asStateFlow()

    private var deadlineMs: Long? = null

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
            return
        }

        val remainingMs = remainingDurationUntil(deadline, nowMs())
        if (remainingMs == null) {
            deadlineMs = null
            mutableState.value = SleepTimerState()
            onExpired()
            return
        }

        mutableState.value = SleepTimerState(remainingMs)
        scheduler.schedule(remainingMs.coerceAtMost(UPDATE_INTERVAL_MS)) { publishRemainingTime() }
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 1_000L
    }
}
