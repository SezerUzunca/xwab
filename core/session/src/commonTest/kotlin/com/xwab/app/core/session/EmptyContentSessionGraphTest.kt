package com.xwab.app.core.session

import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.SleepTimerState
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class EmptyContentSessionGraphTest {
    @Test
    fun aSessionCanBeInjectedWithoutAnyContentContributions() = runBlocking {
        val graph = createGraph<EmptyContentSessionGraph>()
        val engine = assertIs<EmptyContentPlaybackEngine>(graph.engine)
        val removedItem = PlaybackItemId("removed-content", "item")

        graph.session.play(removedItem)

        assertEquals(PlaybackFailure.ItemNotFound(removedItem), graph.session.playback.first().failure)
        assertEquals(0, engine.submittedCommands)
    }
}

// `AppScope` as the app aggregates it once the last content module is gone: nothing on this
// classpath contributes a resolver. The session is reached through the port it is bound to, since
// with `generateContributionProviders` Metro no longer offers the adapter class itself.
@DependencyGraph(AppScope::class)
internal interface EmptyContentSessionGraph {
    val session: PlaybackPort
    val engine: PlaybackEnginePort
}

// Aggregating `AppScope` also brings in the platform engine `core:playback` contributes. A higher
// priority takes its place without naming it, which a test here could not do: that class is
// internal to another module, and a different one on each platform.
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, priority = 1)
internal class EmptyContentPlaybackEngine : PlaybackEnginePort {
    override val state = MutableStateFlow(AudioPlayerState())
    override val sleepTimerState = MutableStateFlow(SleepTimerState())
    var submittedCommands = 0
        private set

    override fun submit(command: PlaybackCommand) {
        submittedCommands++
    }

    override fun release() = Unit
}
