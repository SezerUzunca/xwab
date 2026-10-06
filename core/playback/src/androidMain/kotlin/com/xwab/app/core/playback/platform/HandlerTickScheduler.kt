package com.xwab.app.core.playback.platform

import android.os.Handler
import android.os.Looper
import com.xwab.app.core.playback.timer.TickScheduler
import dev.zacsweers.metro.Inject

/**
 * [TickScheduler] backed by the Android main-looper [Handler].
 *
 * Deliberately unscoped: the sleep-timer countdown and the load timeout each get their own.
 */
@Inject
internal class HandlerTickScheduler : TickScheduler {
    private val handler = Handler(Looper.getMainLooper())
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
