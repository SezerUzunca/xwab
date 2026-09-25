package com.xwab.app.core.playback.projection

import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.PlaybackError
import com.xwab.app.core.playback.port.PlaybackErrorCode
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.port.PlaybackRequest
import com.xwab.app.core.playback.store.DesiredPlayback
import com.xwab.app.core.playback.store.ObservedPlayback
import com.xwab.app.core.playback.store.PlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
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
            desired = DesiredPlayback(
                request = PlaybackRequest(requested),
                isLooping = true,
                playRequested = true,
            ),
            observed = ObservedPlayback(source = attached),
        )

        val projected = project(state)

        assertEquals(requested, projected.requestedSource)
        assertEquals(attached, projected.source)
        assertTrue(projected.playRequested)
        assertTrue(projected.isLooping)
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
