package com.xwab.app.core.playbackengine.store

import com.xwab.app.core.playbackengine.api.AudioSource
import com.xwab.app.core.playbackengine.api.LoopMode
import com.xwab.app.core.playbackengine.api.PlaybackError
import com.xwab.app.core.playbackengine.api.PlaybackErrorCode
import com.xwab.app.core.playbackengine.api.PlaybackRequest

/**
 * The result of a single [reducePlayback] call.
 *
 * [state] is the next immutable state snapshot. [sideEffects] lists the
 * imperative platform operations the driver must execute in order.
 */
internal data class ReduceResult(
    val state: PlaybackState,
    val sideEffects: List<PlaybackSideEffect> = emptyList(),
)

/**
 * Volume round-tripped through a native engine can come back off by a tiny
 * amount. Comparing pending overrides with tolerance prevents an override from
 * never being acknowledged (and being re-applied forever) after such a drift.
 */
private const val VOLUME_TOLERANCE = 0.001f

private fun Float.volumeEquals(other: Float): Boolean =
    kotlin.math.abs(this - other) <= VOLUME_TOLERANCE

/**
 * Pure, deterministic state machine for the audio player.
 *
 * Given the current [PlaybackState] and a [PlaybackMessage], returns the new
 * state and an ordered list of [PlaybackSideEffect]s. The function never
 * touches native APIs, clocks, or I/O — all impurity is confined to the
 * platform driver that executes the returned side effects.
 */
internal fun reducePlayback(
    state: PlaybackState,
    intent: PlaybackMessage,
): ReduceResult {
    if (state.released &&
        intent !is PlaybackMessage.Release &&
        intent !is PlaybackMessage.ControllerConnected
    ) {
        return ReduceResult(state)
    }
    if (intent is PlaybackMessage.SourceOperationEvent &&
        intent.operationId != state.pending.operationId
    ) {
        return ReduceResult(state)
    }
    return when (intent) {
        is PlaybackMessage.Load -> reduceLoad(state, intent.request)
        is PlaybackMessage.Play -> reducePlay(state)
        is PlaybackMessage.Pause -> reducePause(state)
        is PlaybackMessage.SetLooping -> reduceSetLooping(state, intent.enabled)
        is PlaybackMessage.SetVolume -> reduceSetVolume(state, intent.volume)
        is PlaybackMessage.StartSleepTimer -> reduceStartSleepTimer(state, intent)
        is PlaybackMessage.CancelSleepTimer -> reduceCancelSleepTimer(state)
        is PlaybackMessage.Release -> reduceRelease(state)

        is PlaybackMessage.EngineSourceLoaded -> reduceEngineSourceLoaded(state, intent.source)
        is PlaybackMessage.EnginePlaybackEnded -> reduceEnginePlaybackEnded(state)
        is PlaybackMessage.EngineFailed -> reduceEngineFailed(state, intent.error)
        is PlaybackMessage.EngineInterrupted -> reduceEngineInterrupted(state)
        is PlaybackMessage.EnginePlaybackObserved -> reduceEnginePlaybackObserved(state, intent)
        is PlaybackMessage.SleepTimerExpired -> reduceSleepTimerExpired(state)
        is PlaybackMessage.SleepTimerDeadlineObserved ->
            reduceSleepTimerDeadlineObserved(state, intent.deadlineElapsedRealMs)

        is PlaybackMessage.ControllerConnected -> reduceControllerConnected(state, intent)
        is PlaybackMessage.ControllerDisconnected -> reduceControllerDisconnected(state)
        is PlaybackMessage.ControllerConnectionFailed -> reduceControllerConnectionFailed(state)
    }
}

// ── User commands ────────────────────────────────────────────────────────────

private fun reduceLoad(state: PlaybackState, request: PlaybackRequest): ReduceResult {
    check(state.pending.operationId < Long.MAX_VALUE) { "Playback operation id exhausted." }
    val operationId = state.pending.operationId + 1L
    val isLooping = request.loopMode == LoopMode.One
    val newState = state.copy(
        desired = state.desired.copy(
            request = request,
            isLooping = isLooping,
            volume = request.volume,
            playRequested = request.autoplay,
            ended = false,
        ),
        observed = state.observed.copy(source = null, error = null),
        pending = state.pending.copy(
            operationId = operationId,
            pendingSourceOperationId = operationId,
            pendingPlayWhenReady = request.autoplay,
            pendingLooping = isLooping,
            pendingVolume = request.volume,
            rewindRequested = false,
            sourceRecoveryInFlight = false,
        ),
    )
    return ReduceResult(
        newState,
        listOf(PlaybackSideEffect.LoadSource(operationId, request, isLooping)),
    )
}

private fun reducePlay(state: PlaybackState): ReduceResult {
    val newState = state.copy(
        desired = state.desired.copy(playRequested = true, ended = false),
        observed = state.observed.copy(error = null),
        pending = state.pending.copy(pendingPlayWhenReady = true, rewindRequested = false),
    )
    val effect = if (state.desired.ended) {
        PlaybackSideEffect.SeekToStartThenPlay
    } else {
        PlaybackSideEffect.Play
    }
    return ReduceResult(newState, listOf(effect))
}

private fun reducePause(state: PlaybackState): ReduceResult = ReduceResult(
    state.copy(
        desired = state.desired.copy(playRequested = false),
        pending = state.pending.copy(pendingPlayWhenReady = false),
    ),
    listOf(PlaybackSideEffect.Pause),
)

private fun reduceSetLooping(state: PlaybackState, enabled: Boolean): ReduceResult {
    val loopMode = if (enabled) LoopMode.One else LoopMode.Off
    return ReduceResult(
        state.copy(
            desired = state.desired.copy(
                request = state.desired.request?.copy(loopMode = loopMode),
                isLooping = enabled,
            ),
            pending = state.pending.copy(pendingLooping = enabled),
        ),
        listOf(PlaybackSideEffect.SetLooping(enabled)),
    )
}

private fun reduceSetVolume(state: PlaybackState, volume: Float): ReduceResult {
    require(volume.isFinite()) { "Volume must be finite." }
    val safe = volume.coerceIn(0.0f, 1.0f)
    return ReduceResult(
        state.copy(
            desired = state.desired.copy(
                request = state.desired.request?.copy(volume = safe),
                volume = safe,
            ),
            pending = state.pending.copy(pendingVolume = safe),
        ),
        listOf(PlaybackSideEffect.SetVolume(safe)),
    )
}

private fun reduceStartSleepTimer(
    state: PlaybackState,
    intent: PlaybackMessage.StartSleepTimer,
): ReduceResult {
    require(intent.deadlineElapsedRealMs > 0L) { "Sleep timer deadline must be positive." }
    return ReduceResult(
        state.copy(
            sleepTimerLifecycle = SleepTimerLifecycle.Running(intent.deadlineElapsedRealMs),
        ),
        listOf(PlaybackSideEffect.StartSleepTimer(intent.deadlineElapsedRealMs)),
    )
}

private fun reduceCancelSleepTimer(state: PlaybackState): ReduceResult = ReduceResult(
    state.copy(sleepTimerLifecycle = SleepTimerLifecycle.Cancelled),
    listOf(PlaybackSideEffect.CancelSleepTimer),
)

private fun reduceRelease(state: PlaybackState): ReduceResult = ReduceResult(
    state.copy(
        released = true,
        desired = state.desired.copy(playRequested = false),
        sleepTimerLifecycle = SleepTimerLifecycle.Cancelled,
    ),
    listOf(PlaybackSideEffect.Release),
)

// ── Engine events ────────────────────────────────────────────────────────────

private fun reduceEngineSourceLoaded(
    state: PlaybackState,
    source: AudioSource,
): ReduceResult {
    if (state.desired.request?.source != source) return ReduceResult(state)

    val newState = state.copy(
        observed = state.observed.copy(source = source),
        pending = state.pending.copy(pendingSourceOperationId = null),
    )
    val effects = if (state.desired.playRequested) {
        listOf(PlaybackSideEffect.Play)
    } else {
        emptyList()
    }
    return ReduceResult(newState, effects)
}

private fun reduceEngineFailed(
    state: PlaybackState,
    error: PlaybackError,
): ReduceResult = ReduceResult(
    state.copy(
        desired = state.desired.copy(playRequested = false, ended = false),
        observed = state.observed.copy(error = error),
        pending = state.pending.copy(pendingPlayWhenReady = false),
        sleepTimerLifecycle = SleepTimerLifecycle.Cancelled,
    ),
    listOf(PlaybackSideEffect.Pause, PlaybackSideEffect.CancelSleepTimer),
)

private fun reduceEnginePlaybackEnded(state: PlaybackState): ReduceResult {
    return if (state.desired.isLooping) {
        ReduceResult(
            state,
            if (state.desired.playRequested) listOf(PlaybackSideEffect.Play) else emptyList(),
        )
    } else {
        if (state.desired.ended) return ReduceResult(state)
        ReduceResult(
            state.copy(
                desired = state.desired.copy(playRequested = false, ended = true),
                pending = state.pending.copy(pendingPlayWhenReady = false),
                sleepTimerLifecycle = SleepTimerLifecycle.Cancelled,
            ),
            listOf(PlaybackSideEffect.Pause, PlaybackSideEffect.CancelSleepTimer),
        )
    }
}

private fun reduceEngineInterrupted(state: PlaybackState): ReduceResult = ReduceResult(
    state.copy(
        desired = state.desired.copy(playRequested = false),
        observed = state.observed.copy(error = null),
        pending = state.pending.copy(pendingPlayWhenReady = false),
    ),
    listOf(PlaybackSideEffect.PauseForInterruption),
)

private fun reduceEnginePlaybackObserved(
    state: PlaybackState,
    intent: PlaybackMessage.EnginePlaybackObserved,
): ReduceResult {
    val request = state.desired.request
    val requestedSource = request?.source
    if (requestedSource != null && requestedSource != intent.source) {
        val shouldRecoverAttachedSource =
            intent.source != null &&
                state.observed.source == requestedSource &&
                !state.pending.sourceRecoveryInFlight
        return if (shouldRecoverAttachedSource) {
            ReduceResult(
                state.copy(
                    observed = state.observed.copy(source = null),
                    pending = state.pending.copy(
                        sourceRecoveryInFlight = true,
                        pendingSourceOperationId = state.pending.operationId,
                    ),
                ),
                listOf(
                    PlaybackSideEffect.LoadSource(
                        state.pending.operationId,
                        request,
                        state.desired.isLooping,
                    ),
                ),
            )
        } else {
            // A load is already pending, or recovery has already been issued. Ignore the stale
            // observation until the requested source is observed by the native engine.
            ReduceResult(state)
        }
    }

    val playOverrideApplied =
        state.pending.pendingPlayWhenReady?.let { it == intent.playWhenReady } != false
    val loopingOverrideApplied =
        state.pending.pendingLooping?.let { it == intent.looping } != false
    val volumeOverrideApplied =
        state.pending.pendingVolume?.volumeEquals(intent.volume) != false

    return ReduceResult(
        state.copy(
            desired = state.desired.copy(
                playRequested = if (playOverrideApplied) {
                    intent.playWhenReady
                } else {
                    state.desired.playRequested
                },
                isLooping = if (loopingOverrideApplied) {
                    intent.looping
                } else {
                    state.desired.isLooping
                },
                volume = if (volumeOverrideApplied) intent.volume else state.desired.volume,
            ),
            observed = state.observed.copy(source = intent.source),
            pending = state.pending.copy(
                pendingPlayWhenReady = state.pending.pendingPlayWhenReady
                    ?.takeUnless { it == intent.playWhenReady },
                pendingLooping = state.pending.pendingLooping
                    ?.takeUnless { it == intent.looping },
                pendingVolume = state.pending.pendingVolume
                    ?.takeUnless { it.volumeEquals(intent.volume) },
                rewindRequested = if (playOverrideApplied && intent.playWhenReady) {
                    false
                } else {
                    state.pending.rewindRequested
                },
                sourceRecoveryInFlight = false,
                pendingSourceOperationId = state.pending.pendingSourceOperationId
                    ?.takeUnless { it == intent.operationId },
            ),
        ),
    )
}

private fun reduceSleepTimerExpired(state: PlaybackState): ReduceResult = ReduceResult(
    state.copy(
        desired = state.desired.copy(playRequested = false, ended = false),
        pending = state.pending.copy(pendingPlayWhenReady = false, rewindRequested = true),
        sleepTimerLifecycle = SleepTimerLifecycle.Cancelled,
    ),
    listOf(PlaybackSideEffect.Stop, PlaybackSideEffect.CancelSleepTimer),
)

private fun reduceSleepTimerDeadlineObserved(
    state: PlaybackState,
    deadlineElapsedRealMs: Long?,
): ReduceResult = ReduceResult(
    state.copy(
        sleepTimerLifecycle = deadlineElapsedRealMs
            ?.let(SleepTimerLifecycle::Running)
            ?: SleepTimerLifecycle.Cancelled,
    ),
)

// ── Android controller lifecycle ─────────────────────────────────────────────

private fun reduceControllerConnected(
    state: PlaybackState,
    intent: PlaybackMessage.ControllerConnected,
): ReduceResult {
    if (state.released) {
        return ReduceResult(state, listOf(PlaybackSideEffect.Release))
    }

    val pendingRequest = state.desired.request
    val hasUnappliedSourceOperation = state.pending.pendingSourceOperationId != null
    val hasDifferentOwnedOperation =
        pendingRequest != null &&
            state.pending.operationId > 0L &&
            (
                !intent.attachedOperationOwnedByClient ||
                    intent.attachedOperationId != state.pending.operationId
            )
    if (
        pendingRequest != null &&
        (
            pendingRequest.source != intent.attachedSource ||
                hasUnappliedSourceOperation ||
                hasDifferentOwnedOperation
            )
    ) {
        return ReduceResult(
            state.copy(
                observed = state.observed.copy(source = null, error = null),
                pending = state.pending.copy(
                    sourceRecoveryInFlight = true,
                    pendingSourceOperationId = state.pending.operationId,
                ),
            ),
            buildList {
                add(
                    PlaybackSideEffect.LoadSource(
                        state.pending.operationId,
                        pendingRequest,
                        state.desired.isLooping,
                    ),
                )
                addSleepTimerSynchronization(state.sleepTimerLifecycle)
            },
        )
    }

    // Adopt controller values for any setting the user hasn't explicitly overridden.
    val resolvedLooping = state.pending.pendingLooping ?: intent.controllerLooping
    val resolvedVolume = state.pending.pendingVolume ?: intent.controllerVolume
    val resolvedPlay = state.pending.pendingPlayWhenReady ?: intent.controllerPlayWhenReady

    val newState = state.copy(
        desired = state.desired.copy(
            isLooping = resolvedLooping,
            volume = resolvedVolume,
            playRequested = resolvedPlay,
        ),
        observed = state.observed.copy(source = intent.attachedSource, error = null),
        pending = state.pending.copy(
            operationId = intent.attachedOperationId
                ?.takeIf { it > 0L }
                ?: state.pending.operationId,
            pendingSourceOperationId = null,
            pendingLooping = state.pending.pendingLooping
                ?.takeUnless { it == intent.controllerLooping },
            pendingVolume = state.pending.pendingVolume
                ?.takeUnless { it.volumeEquals(intent.controllerVolume) },
            pendingPlayWhenReady = state.pending.pendingPlayWhenReady
                ?.takeUnless { it == intent.controllerPlayWhenReady },
            sourceRecoveryInFlight = false,
        ),
    )

    val effects = mutableListOf<PlaybackSideEffect>()

    // Push user overrides to the controller.
    state.pending.pendingLooping?.let {
        if (it != intent.controllerLooping) effects += PlaybackSideEffect.SetLooping(it)
    }
    state.pending.pendingVolume?.let {
        if (!it.volumeEquals(intent.controllerVolume)) effects += PlaybackSideEffect.SetVolume(it)
    }

    if (state.pending.rewindRequested) {
        effects += PlaybackSideEffect.Stop
    } else {
        state.pending.pendingPlayWhenReady?.let { pwr ->
            if (pwr && !intent.controllerPlayWhenReady) effects += PlaybackSideEffect.Play
            else if (!pwr && intent.controllerPlayWhenReady) effects += PlaybackSideEffect.Pause
        }
    }

    // Sleep timer sync.
    effects.addSleepTimerSynchronization(state.sleepTimerLifecycle)

    return ReduceResult(newState, effects)
}

private fun reduceControllerDisconnected(state: PlaybackState): ReduceResult {
    return ReduceResult(
        state.copy(observed = state.observed.copy(source = null, error = null)),
        emptyList(),
    )
}

private fun reduceControllerConnectionFailed(state: PlaybackState): ReduceResult {
    val error = state.desired.request?.let {
        PlaybackError(
            code = PlaybackErrorCode.ServiceUnavailable,
            message = "Unable to connect to the Android playback service.",
        )
    }
    // A transient connection failure must NOT fabricate a cancel intent: the service and its
    // sleep timer may still be running. Leave the timer lifecycle untouched so the next reconnect
    // restores/re-applies it instead of cancelling a still-running timer.
    return ReduceResult(
        state.copy(
            observed = state.observed.copy(source = null, error = error),
        ),
        emptyList(),
    )
}

private fun MutableList<PlaybackSideEffect>.addSleepTimerSynchronization(
    lifecycle: SleepTimerLifecycle,
) {
    when (lifecycle) {
        SleepTimerLifecycle.Unchanged -> add(PlaybackSideEffect.RestoreSleepTimer)
        SleepTimerLifecycle.Cancelled -> add(PlaybackSideEffect.CancelSleepTimer)
        is SleepTimerLifecycle.Running ->
            add(PlaybackSideEffect.ReconnectSleepTimer(lifecycle.deadlineElapsedRealMs))
    }
}
