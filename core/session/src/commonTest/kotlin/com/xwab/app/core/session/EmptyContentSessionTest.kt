@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.SleepTimerState
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackResolverApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The session once the last content module is removed: no resolver registered for any kind, which
 * leaves the resolver map at the constructor's default, exactly as Metro injects it then.
 *
 * Built directly rather than through a graph. A graph reaches the adapter only through the
 * `PlaybackPort` binding it contributes to `AppScope`, and aggregating `AppScope` here brings the
 * platform engine in too, which clashes with the fake below. Asking a graph for the adapter class
 * itself stopped compiling in Metro 1.4.4, which no longer generates that class's own factory
 * when `generateContributionProviders` is on; 1.4.3 already warned that it would not be visible.
 */
class EmptyContentSessionTest {
    @Test
    fun aSessionWithoutAnyContentModuleReportsTheItemAsNotFound() = runBlocking {
        val engine = EmptyContentPlaybackEngine()
        val session = DefaultPlaybackAdapter(engine)
        val removedItem = PlaybackItemId("removed-content", "item")

        session.play(removedItem)

        assertEquals(PlaybackFailure.ItemNotFound(removedItem), session.playback.first().failure)
        assertEquals(0, engine.submittedCommands)
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
