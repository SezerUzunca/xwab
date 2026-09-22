package com.xwab.app.core.session

import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.SleepTimerState
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class EmptyContentSessionGraphTest {
    @Test
    fun aSessionCanBeInjectedWithoutAnyContentContributions() = runBlocking {
        val engine = EmptyContentPlaybackEngine()
        val graph = createGraphFactory<EmptyContentSessionGraph.Factory>().create(engine)
        val removedItem = PlaybackItemId("removed-content", "item")

        graph.session.play(removedItem)

        assertEquals(PlaybackFailure.ItemNotFound(removedItem), graph.session.playback.first().failure)
        assertEquals(0, engine.submittedCommands)
    }
}

// Own the session's lifetime without aggregating AppScope's platform engine contribution.
@SingleIn(AppScope::class)
@DependencyGraph
internal interface EmptyContentSessionGraph {
    val session: DefaultPlaybackAdapter

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides engine: PlaybackEnginePort): EmptyContentSessionGraph
    }
}

private class EmptyContentPlaybackEngine : PlaybackEnginePort {
    override val state = MutableStateFlow(AudioPlayerState())
    override val sleepTimerState = MutableStateFlow(SleepTimerState())
    var submittedCommands = 0
        private set

    override fun submit(command: PlaybackCommand) {
        submittedCommands++
    }

    override fun release() = Unit
}
