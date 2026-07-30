package com.xwab.app.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Multi-step characterization tests for the Android reconnect + sleep-timer flow.
 *
 * The single-step tests in [PlaybackReducerTest] cover each transition in isolation;
 * these lock the composite behavior across a full load → disconnect/failure → reconnect
 * cycle, so that later on-device hardening of the reconnect/timer paths surfaces any
 * regression here instead of only in manual testing.
 */
class PlaybackReconnectSequenceTest {

    private val sourceA = AudioSource(id = "rain", uri = "file:///rain.mp3")

    /** Loads [sourceA] and marks it attached (as the native engine would report). */
    private fun loadedState(): PlaybackState {
        val loaded = reducePlayback(
            PlaybackState(),
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = true)),
        ).state
        return reducePlayback(
            loaded,
            PlaybackMessage.EngineSourceLoaded(loaded.pending.operationId, sourceA),
        ).state
    }

    private fun reconnect(state: PlaybackState) = reducePlayback(
        state,
        PlaybackMessage.ControllerConnected(
            attachedSource = sourceA,
            attachedOperationId = state.pending.operationId,
            attachedOperationOwnedByClient = true,
            controllerLooping = false,
            controllerVolume = 1.0f,
            controllerPlayWhenReady = true,
        ),
    )

    @Test
    fun sleepTimerSurvivesDisconnectAndReconnect() {
        var state = loadedState()
        state = reducePlayback(
            state,
            PlaybackMessage.StartSleepTimer(deadlineElapsedRealMs = 120_000L),
        ).state
        assertEquals(SleepTimerLifecycle.Running(120_000L), state.sleepTimerLifecycle)

        state = reducePlayback(state, PlaybackMessage.ControllerDisconnected).state
        val result = reconnect(state)

        assertTrue(
            result.sideEffects.any {
                it is PlaybackSideEffect.ReconnectSleepTimer && it.deadlineElapsedRealMs == 120_000L
            },
        )
        assertEquals(SleepTimerLifecycle.Running(120_000L), result.state.sleepTimerLifecycle)
    }

    @Test
    fun sleepTimerSurvivesConnectionFailureThenReconnect() {
        var state = loadedState()
        state = reducePlayback(
            state,
            PlaybackMessage.StartSleepTimer(deadlineElapsedRealMs = 120_000L),
        ).state

        // A transient connection failure must not fabricate a cancel intent.
        state = reducePlayback(state, PlaybackMessage.ControllerConnectionFailed).state
        assertEquals(SleepTimerLifecycle.Running(120_000L), state.sleepTimerLifecycle)

        // The next reconnect re-applies the running timer instead of cancelling it.
        val result = reconnect(state)
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.ReconnectSleepTimer })
        assertFalse(result.sideEffects.any { it is PlaybackSideEffect.CancelSleepTimer })
    }

    @Test
    fun reconnectToSameClientOwnedSourceDoesNotReload() {
        val state = reducePlayback(loadedState(), PlaybackMessage.ControllerDisconnected).state

        val result = reconnect(state)

        // Reconnecting to the same client-owned source must adopt it, not reload it.
        assertFalse(result.sideEffects.any { it is PlaybackSideEffect.LoadSource })
        assertEquals(sourceA, result.state.observed.source)
    }
}
