package com.xwab.app.core.playback.platform

import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.timer.SleepTimerClock
import com.xwab.app.core.playback.timer.TickScheduler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import java.util.concurrent.Executor

/**
 * This module's own graph on Android. Only [PlaybackEnginePort] reaches the application graph; the
 * [Context] the engine connects to the playback service with comes in through the factory.
 */
@DependencyGraph(PlaybackScope::class)
internal interface AndroidPlaybackGraph {
    val engine: PlaybackEnginePort

    @Binds val AndroidPlaybackFacade.bindEngine: PlaybackEnginePort

    @Binds val HandlerTickScheduler.bindTickScheduler: TickScheduler

    /** Where controller and timer replies arrive: the main thread the engine runs on. */
    @Provides
    fun provideMainExecutor(context: Context): Executor =
        ContextCompat.getMainExecutor(context.applicationContext)

    /**
     * `elapsedRealtime`, because the sleep-timer deadline is shared with [PlaybackService], which
     * owns the timer and counts in that clock; it also keeps running while the device sleeps.
     */
    @Provides
    fun provideSleepTimerClock(): SleepTimerClock = SleepTimerClock(SystemClock::elapsedRealtime)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidPlaybackGraph
    }
}

/**
 * Hands the module graph's engine to the application graph, which builds it once — on the main
 * thread, as the engine requires.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidPlaybackGraphAdapter(
    context: Context,
) : PlaybackEnginePort by createGraphFactory<AndroidPlaybackGraph.Factory>().create(context).engine
