package com.xwab.app.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the shared model → published-state projection both facades use, so the two
 * platforms cannot drift apart on which field is model-derived and which is live.
 */
class PlaybackProjectionTest {

    private val requested = AudioSource(id = "rain", uri = "file:///rain.mp3")
    private val attached = AudioSource(id = "ocean", uri = "file:///ocean.mp3")

    private fun project(
        state: PlaybackState,
        phase: PlaybackPhase = PlaybackPhase.Ready,
        isPlaying: Boolean = false,
        volume: Float = 1.0f,
        error: PlaybackError? = null,
    ) = projectPlaybackState(state, phase, isPlaying, volume, error)

    @Test
    fun modelDerivedFieldsComeFromTheirOwnConcern() {
        val state = PlaybackState(
            desired = DesiredPlayback(request = PlaybackRequest(requested), playRequested = true),
            observed = ObservedPlayback(source = attached),
        )

        val projected = project(state)

        assertEquals(requested, projected.requestedSource)
        assertEquals(attached, projected.source)
        assertTrue(projected.playRequested)
    }

    @Test
    fun liveEngineValuesArePassedThrough() {
        val error = PlaybackError(PlaybackErrorCode.PlaybackFailed, "boom")

        val projected = project(
            state = PlaybackState(),
            phase = PlaybackPhase.Buffering,
            isPlaying = true,
            volume = 0.25f,
            error = error,
        )

        assertEquals(PlaybackPhase.Buffering, projected.phase)
        assertTrue(projected.isPlaying)
        assertEquals(0.25f, projected.volume)
        assertEquals(error, projected.error)
    }

    @Test
    fun loopingIsTheReconciledModelValueNotAPendingOverride() {
        // The reducer keeps the requested value in `desired` while the engine has not
        // acknowledged it yet; the published state must show that, not the stale flag.
        val state = PlaybackState(
            desired = DesiredPlayback(request = PlaybackRequest(requested), isLooping = true),
            observed = ObservedPlayback(source = requested),
            pending = PendingReconciliation(pendingLooping = true),
        )

        assertTrue(project(state).isLooping)
    }

    @Test
    fun loopingFollowsTheModelWhenTheUserTurnsItOff() {
        val state = PlaybackState(
            desired = DesiredPlayback(request = PlaybackRequest(requested), isLooping = false),
            observed = ObservedPlayback(source = requested),
        )

        assertFalse(project(state).isLooping)
    }

    @Test
    fun activeSourcePrefersTheAttachedSourceThenTheRequestedOne() {
        val reconnecting = PlaybackState(
            desired = DesiredPlayback(request = PlaybackRequest(requested)),
            observed = ObservedPlayback(source = null),
        )
        assertEquals(requested, project(reconnecting).activeSource)

        val attachedState = PlaybackState(
            desired = DesiredPlayback(request = PlaybackRequest(requested)),
            observed = ObservedPlayback(source = attached),
        )
        assertEquals(attached, project(attachedState).activeSource)
    }
}
