package com.xwab.app.core.media.platform

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** Private MediaSession custom command protocol between [SleepTimerClient] and [PlaybackService]. */
internal object SleepTimerProtocol {
    const val ACTION_START = "com.xwab.app.core.media.action.START_SLEEP_TIMER"
    const val ACTION_CANCEL = "com.xwab.app.core.media.action.CANCEL_SLEEP_TIMER"
    const val ACTION_GET_STATE = "com.xwab.app.core.media.action.GET_SLEEP_TIMER_STATE"

    private const val EXTRA_REQUESTED_DEADLINE_ELAPSED_REALTIME_MS =
        "sleep_timer_requested_deadline_elapsed_realtime_ms"
    private const val EXTRA_DEADLINE_ELAPSED_REALTIME_MS = "sleep_timer_deadline_elapsed_realtime_ms"
    private const val NO_TIMER_DEADLINE = -1L

    fun command(action: String): SessionCommand = SessionCommand(action, Bundle.EMPTY)

    fun startArguments(deadlineElapsedRealtimeMs: Long): Bundle = Bundle().apply {
        putLong(EXTRA_REQUESTED_DEADLINE_ELAPSED_REALTIME_MS, deadlineElapsedRealtimeMs)
    }

    fun requestedDeadlineFrom(arguments: Bundle): Long =
        arguments.getLong(EXTRA_REQUESTED_DEADLINE_ELAPSED_REALTIME_MS, 0L)

    fun stateArguments(deadlineElapsedRealtimeMs: Long?): Bundle = Bundle().apply {
        putLong(EXTRA_DEADLINE_ELAPSED_REALTIME_MS, deadlineElapsedRealtimeMs ?: NO_TIMER_DEADLINE)
    }

    fun deadlineFrom(arguments: Bundle): Long? = arguments
        .getLong(EXTRA_DEADLINE_ELAPSED_REALTIME_MS, NO_TIMER_DEADLINE)
        .takeIf { it > 0L }
}
