package com.xwab.app.core.playback.platform

import android.os.Handler
import com.xwab.app.core.playback.timer.TickScheduler
import dev.zacsweers.metro.Inject

/**
 * [TickScheduler] backed by a main-looper [Handler].
 *
 * Deliberately unscoped: the sleep-timer countdown, the load timeout and the service's timer each
 * get their own.
 */
@Inject
internal class HandlerTickScheduler(
    private val handler: Handler,
) : TickScheduler {
    private var pendingTick: Runnable? = null

    override fun schedule(delayMs: Long, action: () -> Unit) {
        cancel()
        val tick = Runnable {
            pendingTick = null
            action()
        }
        pendingTick = tick
        handler.postDelayed(tick, delayMs)
    }

    override fun cancel() {
        pendingTick?.let(handler::removeCallbacks)
        pendingTick = null
    }
}
