package com.xwab.app.core.playback.platform

import com.xwab.app.core.playback.timer.TickScheduler
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * [TickScheduler] backed by a coroutine scope on the main dispatcher, which keeps the tick
 * callback on the player thread this module requires on iOS. Unscoped, like every scheduler.
 *
 * The dispatcher is injected; the scope is this scheduler's own, so [release] can cancel it.
 */
@Inject
internal class CoroutineTickScheduler(
    mainDispatcher: CoroutineDispatcher,
) : TickScheduler {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
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
