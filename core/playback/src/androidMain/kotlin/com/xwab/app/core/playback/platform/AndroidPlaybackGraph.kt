package com.xwab.app.core.playback.platform

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.SessionToken
import com.xwab.app.core.playback.port.PlaybackEnginePort
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
@DependencyGraph(PlaybackScope::class, bindingContainers = [AndroidPlaybackBindings::class])
internal interface AndroidPlaybackGraph {
    val engine: PlaybackEnginePort

    @Binds val AndroidPlaybackFacade.bindEngine: PlaybackEnginePort

    /** Where controller and timer replies arrive: the main thread the engine runs on. */
    @Provides
    fun provideMainExecutor(context: Context): Executor =
        ContextCompat.getMainExecutor(context.applicationContext)

    /** The session the engine's controller connects to: this module's [PlaybackService]. */
    @Provides
    fun provideSessionToken(context: Context): SessionToken {
        val appContext = context.applicationContext
        return SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    }

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
