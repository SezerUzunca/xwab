package com.xwab.app.core.playback.platform

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
import dev.zacsweers.metro.createGraph
import kotlin.time.TimeSource
import platform.Foundation.NSBundle

/** The application owns its identity; the reusable player only reads its configuration. */
private const val USER_AGENT_METADATA_KEY = "com.xwab.app.core.playback.USER_AGENT"

/** This module's own graph on iOS. Only [PlaybackEnginePort] reaches the application graph. */
@DependencyGraph(PlaybackScope::class)
internal interface IosPlaybackGraph {
    val engine: PlaybackEnginePort

    @Binds val IosPlaybackFacade.bindEngine: PlaybackEnginePort

    @Binds val CoroutineTickScheduler.bindTickScheduler: TickScheduler

    /**
     * A process-local monotonic clock. iOS owns its sleep timer in-process, so it derives its own
     * deadlines; one clock per graph keeps the facade that sets a deadline and the countdown that
     * reads it on the same origin.
     */
    @Provides
    @SingleIn(PlaybackScope::class)
    fun provideSleepTimerClock(): SleepTimerClock {
        val origin = TimeSource.Monotonic.markNow()
        return SleepTimerClock { origin.elapsedNow().inWholeMilliseconds }
    }

    @Provides
    @ApplicationUserAgent
    fun provideUserAgent(): String? =
        NSBundle.mainBundle.objectForInfoDictionaryKey(USER_AGENT_METADATA_KEY) as? String
}

/**
 * Hands the module graph's engine to the application graph, which builds it once — on the main
 * thread, as the engine requires.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosPlaybackGraphAdapter : PlaybackEnginePort by createGraph<IosPlaybackGraph>().engine
