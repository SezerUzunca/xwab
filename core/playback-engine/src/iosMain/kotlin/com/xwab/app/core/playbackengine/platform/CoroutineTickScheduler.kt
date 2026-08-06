package com.xwab.app.core.playbackengine.platform

import com.xwab.app.core.playbackengine.timer.TickScheduler
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * [TickScheduler] backed by a main-thread coroutine scope, which keeps the tick
 * callback on the player thread this module requires on iOS.
 */
internal class CoroutineTickScheduler : TickScheduler {
    private val scope = MainScope()
    private var tickJob: Job? = null

    override fun schedule(delayMs: Long, action: () -> Unit) {
        cancel()
        tickJob = scope.launch {
            delay(delayMs.milliseconds)
            tickJob = null
            action()
        }
    }

    override fun cancel() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun release() {
        cancel()
        scope.cancel()
    }
}
