package com.xwab.app.core.media

/**
 * Derives the published [AudioPlayerState] from the reducer's [PlaybackState] plus
 * the live values only a native engine can answer for.
 *
 * Both platform facades project through this function so that "which field comes
 * from the model and which from the engine" is decided once instead of per driver.
 *
 * @param phase     Platform-mapped playback phase.
 * @param isPlaying Whether the native engine is actually producing audio right now.
 * @param volume    The engine's current volume.
 * @param error     The error to publish; a driver may merge a live engine error on
 *                  top of the reducer's [ObservedPlayback.error].
 */
internal fun projectPlaybackState(
    state: PlaybackState,
    phase: PlaybackPhase,
    isPlaying: Boolean,
    volume: Float,
    error: PlaybackError?,
): AudioPlayerState = AudioPlayerState(
    requestedSource = state.desired.request?.source,
    source = state.observed.source,
    phase = phase,
    playRequested = state.desired.playRequested,
    isPlaying = isPlaying,
    // Deliberately the reconciled model value, not the raw engine flag: the reducer
    // already adopts engine/remote loop changes (EnginePlaybackObserved,
    // ControllerConnected) and holds the requested value while an override is still
    // pending. Publishing the engine flag directly would bypass that reconciliation
    // and make the loop toggle flip back until the engine acknowledges.
    isLooping = state.desired.isLooping,
    volume = volume,
    error = error,
)
