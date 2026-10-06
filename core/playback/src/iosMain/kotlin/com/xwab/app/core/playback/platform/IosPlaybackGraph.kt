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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVQueuePlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPRemoteCommandCenter

/** The application owns its identity; the reusable player only reads its configuration. */
private const val USER_AGENT_METADATA_KEY = "com.xwab.app.core.playback.USER_AGENT"

/**
 * This module's own graph on iOS. Only [PlaybackEnginePort] reaches the application graph.
 *
 * The system's shared centers are looked up here, so the parts that use them take them as inputs.
 * None of these providers runs before the facade's main-thread check: every part that asks for
 * one is created through a factory or a provider.
 */
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

    /** The player thread: every tick runs where the engine requires. */
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @ApplicationUserAgent
    fun provideUserAgent(): String? =
        NSBundle.mainBundle.objectForInfoDictionaryKey(USER_AGENT_METADATA_KEY) as? String

    /** Unscoped: the engine asks for a new player after a media-services reset. */
    @Provides
    fun provideQueuePlayer(): AVQueuePlayer = AVQueuePlayer()

    @Provides
    fun provideAudioSession(): AVAudioSession = AVAudioSession.sharedInstance()

    @Provides
    fun provideNotificationCenter(): NSNotificationCenter = NSNotificationCenter.defaultCenter

    @Provides
    fun provideRemoteCommandCenter(): MPRemoteCommandCenter = MPRemoteCommandCenter.sharedCommandCenter()

    @Provides
    fun provideNowPlayingInfoCenter(): MPNowPlayingInfoCenter = MPNowPlayingInfoCenter.defaultCenter()
}

/**
 * Hands the module graph's engine to the application graph, which builds it once — on the main
 * thread, as the engine requires.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosPlaybackGraphAdapter : PlaybackEnginePort by createGraph<IosPlaybackGraph>().engine
