package com.xwab.app.core.media.platform

import android.os.Handler
import android.os.Looper

/** [TickScheduler] backed by the Android main-looper [Handler]. */
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
import com.xwab.app.core.media.timer.TickScheduler
