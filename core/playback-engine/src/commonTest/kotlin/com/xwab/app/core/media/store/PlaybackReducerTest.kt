package com.xwab.app.core.media.store

import com.xwab.app.core.media.api.AudioSource
import com.xwab.app.core.media.api.LoopMode
import com.xwab.app.core.media.api.PlaybackCommand
import com.xwab.app.core.media.api.PlaybackError
import com.xwab.app.core.media.api.PlaybackErrorCode
import com.xwab.app.core.media.api.PlaybackPhase
import com.xwab.app.core.media.api.PlaybackRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun playbackState(
    request: PlaybackRequest? = null,
    source: AudioSource? = null,
    isLooping: Boolean = false,
    volume: Float = 1.0f,
    error: PlaybackError? = null,
    released: Boolean = false,
    operationId: Long = 0L,
    pendingSourceOperationId: Long? = null,
    playRequested: Boolean = false,
    ended: Boolean = false,
    pendingPlayWhenReady: Boolean? = null,
    pendingLooping: Boolean? = null,
    pendingVolume: Float? = null,
    rewindRequested: Boolean = false,
    sourceRecoveryInFlight: Boolean = false,
    sleepTimerLifecycle: SleepTimerLifecycle = SleepTimerLifecycle.Unchanged,
): PlaybackState = PlaybackState(
    desired = DesiredPlayback(
        request = request,
        isLooping = isLooping,
        volume = volume,
        playRequested = playRequested,
        ended = ended,
    ),
    observed = ObservedPlayback(source = source, error = error),
    pending = PendingReconciliation(
        operationId = operationId,
        pendingSourceOperationId = pendingSourceOperationId,
        pendingPlayWhenReady = pendingPlayWhenReady,
        pendingLooping = pendingLooping,
        pendingVolume = pendingVolume,
        rewindRequested = rewindRequested,
        sourceRecoveryInFlight = sourceRecoveryInFlight,
    ),
    sleepTimerLifecycle = sleepTimerLifecycle,
    released = released,
)

class PlaybackReducerTest {

    private val sourceA = AudioSource(id = "rain", uri = "file:///rain.mp3")
    private val sourceB = AudioSource(id = "ocean", uri = "file:///ocean.mp3")

    // ── Load ─────────────────────────────────────────────────────────────

    @Test
    fun loadResetsLifecycleAndEmitsLoadSource() {
        val request = PlaybackRequest(
            source = sourceA,
            autoplay = true,
            loopMode = LoopMode.One,
            volume = 0.5f,
        )
        val result = reducePlayback(PlaybackState(), PlaybackMessage.Load(request))

        assertEquals(request, result.state.desired.request)
        assertNull(result.state.observed.source)
        assertTrue(result.state.desired.isLooping)
        assertEquals(0.5f, result.state.desired.volume)
        assertTrue(result.state.desired.playRequested)
        assertFalse(result.state.desired.ended)
        assertNull(result.state.observed.error)

        val load = result.sideEffects.filterIsInstance<PlaybackSideEffect.LoadSource>().single()
        assertEquals(result.state.pending.operationId, load.operationId)
        assertEquals(
            result.state.pending.operationId,
            result.state.pending.pendingSourceOperationId,
        )
        assertEquals(request, load.request)
        assertTrue(load.isLooping)
    }

    @Test
    fun consecutiveLoadsKeepLatestDesiredRequest() {
        val first = PlaybackRequest(sourceA, autoplay = true, loopMode = LoopMode.One, volume = 0.2f)
        val latest = PlaybackRequest(sourceB, autoplay = false, loopMode = LoopMode.Off, volume = 0.8f)

        val r1 = reducePlayback(PlaybackState(), PlaybackMessage.Load(first))
        val r2 = reducePlayback(r1.state, PlaybackMessage.Load(latest))

        assertEquals(latest, r2.state.desired.request)
        assertFalse(r2.state.desired.isLooping)
        assertEquals(0.8f, r2.state.desired.volume)
        assertEquals(false, r2.state.pending.pendingPlayWhenReady)
        assertEquals(r1.state.pending.operationId + 1L, r2.state.pending.operationId)
        assertEquals(
            r2.state.pending.operationId,
            r2.sideEffects.filterIsInstance<PlaybackSideEffect.LoadSource>().single().operationId,
        )
    }

    @Test
    fun failureFromSupersededLoadOfSameSourceIsIgnored() {
        val first = reducePlayback(
            PlaybackState(),
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = true)),
        )
        val latest = reducePlayback(
            first.state,
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = false)),
        )
        val staleFailure = PlaybackMessage.EngineFailed(
            operationId = first.state.pending.operationId,
            error = PlaybackError(PlaybackErrorCode.PlaybackFailed, "Old operation failed."),
        )

        val result = reducePlayback(latest.state, staleFailure)

        assertEquals(latest.state, result.state)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun endFromSupersededLoadOfSameSourceIsIgnored() {
        val first = reducePlayback(
            PlaybackState(),
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = true)),
        )
        val latest = reducePlayback(
            first.state,
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = true)),
        )

        val result = reducePlayback(
            latest.state,
            PlaybackMessage.EnginePlaybackEnded(first.state.pending.operationId),
        )

        assertEquals(latest.state, result.state)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun playbackObservationFromSupersededLoadOfSameSourceIsIgnored() {
        val first = reducePlayback(
            PlaybackState(),
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = true)),
        )
        val latest = reducePlayback(
            first.state,
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = false)),
        )

        val result = reducePlayback(
            latest.state,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = first.state.pending.operationId,
                source = sourceA,
                playWhenReady = true,
                looping = false,
                volume = 1.0f,
            ),
        )

        assertEquals(latest.state, result.state)
        assertTrue(result.sideEffects.isEmpty())
    }

    // ── Play ─────────────────────────────────────────────────────────────

    @Test
    fun playSetsPlayRequestedAndEmitsPlay() {
        val state = playbackState(source = sourceA)
        val result = reducePlayback(state, PlaybackMessage.Play)

        assertTrue(result.state.desired.playRequested)
        assertFalse(result.state.desired.ended)
        assertNull(result.state.observed.error)
        assertIs<PlaybackSideEffect.Play>(result.sideEffects.single())
    }

    @Test
    fun playAfterEndedEmitsSeekToStartThenPlay() {
        val state = playbackState(source = sourceA, ended = true)
        val result = reducePlayback(state, PlaybackMessage.Play)

        assertTrue(result.state.desired.playRequested)
        assertFalse(result.state.desired.ended)
        assertIs<PlaybackSideEffect.SeekToStartThenPlay>(result.sideEffects.single())
    }

    @Test
    fun playClearsRewindRequest() {
        val state = playbackState(rewindRequested = true)
        val result = reducePlayback(state, PlaybackMessage.Play)

        assertFalse(result.state.pending.rewindRequested)
        assertTrue(result.state.pending.pendingPlayWhenReady == true)
    }

    // ── Pause ────────────────────────────────────────────────────────────

    @Test
    fun pauseClearsPlayRequestedAndEmitsPause() {
        val state = playbackState(playRequested = true)
        val result = reducePlayback(state, PlaybackMessage.Pause)

        assertFalse(result.state.desired.playRequested)
        assertEquals(false, result.state.pending.pendingPlayWhenReady)
        assertIs<PlaybackSideEffect.Pause>(result.sideEffects.single())
    }

    // ── SetLooping ───────────────────────────────────────────────────────

    @Test
    fun setLoopingUpdatesStateAndEmitsEffect() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, loopMode = LoopMode.Off),
        )
        val result = reducePlayback(state, PlaybackMessage.SetLooping(true))

        assertTrue(result.state.desired.isLooping)
        assertEquals(LoopMode.One, result.state.desired.request?.loopMode)
        assertTrue(result.state.pending.pendingLooping == true)
        val eff = result.sideEffects.filterIsInstance<PlaybackSideEffect.SetLooping>().single()
        assertTrue(eff.enabled)
    }

    // ── SetVolume ────────────────────────────────────────────────────────

    @Test
    fun setVolumeClampsAndUpdatesState() {
        val state = playbackState(request = PlaybackRequest(sourceA, volume = 1.0f))
        val result = reducePlayback(state, PlaybackMessage.SetVolume(1.5f))

        assertEquals(1.0f, result.state.desired.volume)
        assertEquals(1.0f, result.state.desired.request?.volume)
        assertEquals(1.0f, result.state.pending.pendingVolume)
    }

    @Test
    fun setVolumeRejectsNaNDeterministically() {
        assertFailsWith<IllegalArgumentException> {
            reducePlayback(PlaybackState(), PlaybackMessage.SetVolume(Float.NaN))
        }
    }

    // ── StartSleepTimer / CancelSleepTimer ───────────────────────────────

    @Test
    fun startSleepTimerSetsRunningLifecycle() {
        val result = reducePlayback(
            PlaybackState(),
            PlaybackMessage.StartSleepTimer(deadlineElapsedRealMs = 100_000L),
        )
        val lifecycle = result.state.sleepTimerLifecycle
        assertIs<SleepTimerLifecycle.Running>(lifecycle)
        assertEquals(100_000L, lifecycle.deadlineElapsedRealMs)
        val eff = result.sideEffects.filterIsInstance<PlaybackSideEffect.StartSleepTimer>().single()
        assertEquals(100_000L, eff.deadlineElapsedRealMs)
    }

    @Test
    fun startSleepTimerRejectsAnInvalidDeadlineBeforeChangingState() {
        assertFailsWith<IllegalArgumentException> {
            reducePlayback(
                PlaybackState(),
                PlaybackMessage.StartSleepTimer(deadlineElapsedRealMs = 0L),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SleepTimerLifecycle.Running(0L)
        }
    }

    @Test
    fun publicTimerCommandRequiresAndPreservesAValidatedDeadline() {
        val command = PlaybackCommand.StartSleepTimer(durationMs = 60_000L)

        assertFailsWith<IllegalArgumentException> { command.toMessage() }
        assertEquals(
            PlaybackMessage.StartSleepTimer(75_000L),
            command.toMessage(sleepTimerDeadlineElapsedRealMs = 75_000L),
        )
    }

    @Test
    fun cancelSleepTimerSetsCancelledLifecycle() {
        val state = playbackState(
            sleepTimerLifecycle = SleepTimerLifecycle.Running(50_000L),
        )
        val result = reducePlayback(state, PlaybackMessage.CancelSleepTimer)
        assertIs<SleepTimerLifecycle.Cancelled>(result.state.sleepTimerLifecycle)
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.CancelSleepTimer })
    }

    // ── Release ──────────────────────────────────────────────────────────

    @Test
    fun releaseSetsReleasedAndCancelsSleepTimer() {
        val result = reducePlayback(playbackState(playRequested = true), PlaybackMessage.Release)

        assertTrue(result.state.released)
        assertFalse(result.state.desired.playRequested)
        assertIs<SleepTimerLifecycle.Cancelled>(result.state.sleepTimerLifecycle)
        assertIs<PlaybackSideEffect.Release>(result.sideEffects.single())
    }

    @Test
    fun intentsAreIgnoredAfterRelease() {
        val released = PlaybackState(released = true)
        val result = reducePlayback(released, PlaybackMessage.Play)

        assertEquals(released, result.state)
        assertTrue(result.sideEffects.isEmpty())
    }

    // ── Engine events ────────────────────────────────────────────────────

    @Test
    fun engineSourceLoadedSetsSourceAndAutoPlays() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, autoplay = true),
            playRequested = true,
        )
        val result = reducePlayback(
            state,
            PlaybackMessage.EngineSourceLoaded(state.pending.operationId, sourceA),
        )

        assertEquals(sourceA, result.state.observed.source)
        assertTrue(result.state.desired.playRequested)
        assertIs<PlaybackSideEffect.Play>(result.sideEffects.single())
    }

    @Test
    fun engineSourceLoadedDoesNotAutoPlayIfNotRequested() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, autoplay = false),
        )
        val result = reducePlayback(
            state,
            PlaybackMessage.EngineSourceLoaded(state.pending.operationId, sourceA),
        )

        assertEquals(sourceA, result.state.observed.source)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun enginePlaybackEndedStopsNonLoopingAndCancelsTimer() {
        val state = playbackState(playRequested = true, isLooping = false)
        val result = reducePlayback(
            state,
            PlaybackMessage.EnginePlaybackEnded(state.pending.operationId),
        )

        assertFalse(result.state.desired.playRequested)
        assertTrue(result.state.desired.ended)
        assertEquals(false, result.state.pending.pendingPlayWhenReady)
        assertIs<SleepTimerLifecycle.Cancelled>(result.state.sleepTimerLifecycle)
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.Pause })
    }

    @Test
    fun enginePlaybackEndedContinuesLooping() {
        val state = playbackState(playRequested = true, isLooping = true)
        val result = reducePlayback(
            state,
            PlaybackMessage.EnginePlaybackEnded(state.pending.operationId),
        )

        assertTrue(result.state.desired.playRequested) // unchanged
        assertFalse(result.state.desired.ended)
        assertIs<PlaybackSideEffect.Play>(result.sideEffects.single())
    }

    @Test
    fun engineFailedSetsErrorAndCancelsTimer() {
        val error = PlaybackError(PlaybackErrorCode.PlaybackFailed, "Broken pipe")
        val state = playbackState(playRequested = true)
        val result = reducePlayback(
            state,
            PlaybackMessage.EngineFailed(state.pending.operationId, error),
        )

        assertEquals(error, result.state.observed.error)
        assertFalse(result.state.desired.playRequested)
        assertEquals(false, result.state.pending.pendingPlayWhenReady)
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.Pause })
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.CancelSleepTimer })
    }

    @Test
    fun engineInterruptedIsNonFatalSoftPause() {
        val state = playbackState(playRequested = true, source = sourceA)
        val result = reducePlayback(state, PlaybackMessage.EngineInterrupted)

        assertFalse(result.state.desired.playRequested)
        assertNull(result.state.observed.error)
        assertIs<PlaybackSideEffect.PauseForInterruption>(result.sideEffects.single())
    }

    @Test
    fun sleepTimerExpiredStopsPlayback() {
        val state = playbackState(
            playRequested = true,
            sleepTimerLifecycle = SleepTimerLifecycle.Running(50_000L),
        )
        val result = reducePlayback(state, PlaybackMessage.SleepTimerExpired)

        assertFalse(result.state.desired.playRequested)
        assertIs<SleepTimerLifecycle.Cancelled>(result.state.sleepTimerLifecycle)
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.Stop })
    }

    @Test
    fun staleSourceLoadedEventIsIgnored() {
        val current = PlaybackRequest(sourceB, autoplay = true)
        val state = playbackState(request = current, playRequested = true)

        val result = reducePlayback(
            state,
            PlaybackMessage.EngineSourceLoaded(state.pending.operationId, sourceA),
        )

        assertEquals(state, result.state)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun observedPlaybackAcknowledgesOverridesAndAdoptsLaterRemotePause() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, autoplay = true),
            source = sourceA,
            operationId = 3L,
            pendingSourceOperationId = 3L,
            playRequested = true,
            pendingPlayWhenReady = true,
            pendingLooping = true,
            pendingVolume = 0.4f,
        )
        val acknowledged = reducePlayback(
            state,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = state.pending.operationId,
                source = sourceA,
                playWhenReady = true,
                looping = true,
                volume = 0.4f,
            ),
        ).state

        assertNull(acknowledged.pending.pendingPlayWhenReady)
        assertNull(acknowledged.pending.pendingLooping)
        assertNull(acknowledged.pending.pendingVolume)
        assertNull(acknowledged.pending.pendingSourceOperationId)

        val remotelyPaused = reducePlayback(
            acknowledged,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = acknowledged.pending.operationId,
                source = sourceA,
                playWhenReady = false,
                looping = true,
                volume = 0.4f,
            ),
        ).state
        assertFalse(remotelyPaused.desired.playRequested)
        assertNull(remotelyPaused.pending.pendingPlayWhenReady)
    }

    @Test
    fun observationDoesNotOverwriteAnOverrideThatHasNotBeenAppliedYet() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, autoplay = true, volume = 0.4f),
            source = sourceA,
            playRequested = true,
            isLooping = true,
            volume = 0.4f,
            pendingPlayWhenReady = true,
            pendingLooping = true,
            pendingVolume = 0.4f,
        )

        val observedOldValues = reducePlayback(
            state,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = state.pending.operationId,
                source = sourceA,
                playWhenReady = false,
                looping = false,
                volume = 1.0f,
            ),
        ).state

        assertTrue(observedOldValues.desired.playRequested)
        assertTrue(observedOldValues.desired.isLooping)
        assertEquals(0.4f, observedOldValues.desired.volume)
        assertEquals(true, observedOldValues.pending.pendingPlayWhenReady)
        assertEquals(true, observedOldValues.pending.pendingLooping)
        assertEquals(0.4f, observedOldValues.pending.pendingVolume)
    }

    @Test
    fun unexpectedObservedSourceReloadsAttachedRequestOnlyOnce() {
        val request = PlaybackRequest(sourceA, autoplay = true, loopMode = LoopMode.One)
        val state = playbackState(
            request = request,
            source = sourceA,
            playRequested = true,
            isLooping = true,
        )

        val recovery = reducePlayback(
            state,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = state.pending.operationId,
                source = sourceB,
                playWhenReady = true,
                looping = false,
                volume = 1.0f,
            ),
        )

        assertNull(recovery.state.observed.source)
        assertTrue(recovery.state.pending.sourceRecoveryInFlight)
        assertEquals(
            request,
            recovery.sideEffects.filterIsInstance<PlaybackSideEffect.LoadSource>().single().request,
        )

        val sourceCommandIssued = reducePlayback(
            recovery.state,
            PlaybackMessage.EngineSourceLoaded(recovery.state.pending.operationId, sourceA),
        ).state
        val duplicateObservation = reducePlayback(
            sourceCommandIssued,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = sourceCommandIssued.pending.operationId,
                source = sourceB,
                playWhenReady = true,
                looping = false,
                volume = 1.0f,
            ),
        )
        assertTrue(duplicateObservation.sideEffects.isEmpty())
        assertTrue(duplicateObservation.state.pending.sourceRecoveryInFlight)
    }

    @Test
    fun staleObservedSourceIsIgnoredWhileRequestedLoadIsPending() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, autoplay = true),
            source = null,
            playRequested = true,
            pendingPlayWhenReady = true,
        )

        val result = reducePlayback(
            state,
            PlaybackMessage.EnginePlaybackObserved(
                operationId = state.pending.operationId,
                source = sourceB,
                playWhenReady = false,
                looping = false,
                volume = 1.0f,
            ),
        )

        assertEquals(state, result.state)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun timerDeadlineObservationKeepsReducerInSyncWithService() {
        val running = reducePlayback(
            PlaybackState(),
            PlaybackMessage.SleepTimerDeadlineObserved(50_000L),
        ).state
        assertEquals(SleepTimerLifecycle.Running(50_000L), running.sleepTimerLifecycle)

        val cancelled = reducePlayback(
            running,
            PlaybackMessage.SleepTimerDeadlineObserved(null),
        ).state
        assertIs<SleepTimerLifecycle.Cancelled>(cancelled.sleepTimerLifecycle)
    }

    // ── Android controller events ────────────────────────────────────────

    @Test
    fun controllerConnectedLoadsIfSourceDiffers() {
        val request = PlaybackRequest(sourceA)
        val state = playbackState(
            request = request,
            isLooping = true,
            sleepTimerLifecycle = SleepTimerLifecycle.Running(75_000L),
        )

        val result = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceB,
                controllerLooping = false,
                controllerVolume = 0.5f,
                controllerPlayWhenReady = false,
            ),
        )

        val load = result.sideEffects.filterIsInstance<PlaybackSideEffect.LoadSource>().single()
        assertEquals(request, load.request)
        assertTrue(
            result.sideEffects.any {
                it is PlaybackSideEffect.ReconnectSleepTimer &&
                    it.deadlineElapsedRealMs == 75_000L
            },
        )
    }

    @Test
    fun controllerConnectedAdoptsControllerStateWhenSourceMatches() {
        val state = playbackState(
            request = PlaybackRequest(sourceA),
            sleepTimerLifecycle = SleepTimerLifecycle.Unchanged,
        )
        val result = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                controllerLooping = true,
                controllerVolume = 0.7f,
                controllerPlayWhenReady = true,
            ),
        )

        assertEquals(sourceA, result.state.observed.source)
        assertTrue(result.state.desired.isLooping)
        assertEquals(0.7f, result.state.desired.volume)
        assertTrue(result.state.desired.playRequested)
        assertNull(result.state.pending.pendingLooping)
        assertNull(result.state.pending.pendingVolume)
        assertNull(result.state.pending.pendingPlayWhenReady)
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.RestoreSleepTimer })
    }

    @Test
    fun controllerConnectedReloadsAppliedOperationWhenOwnerDoesNotMatch() {
        val pending = reducePlayback(
            playbackState(operationId = 1L),
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = false)),
        )
        val applied = reducePlayback(
            pending.state,
            PlaybackMessage.EngineSourceLoaded(
                operationId = pending.state.pending.operationId,
                source = sourceA,
            ),
        ).state
        assertNull(applied.pending.pendingSourceOperationId)

        val connected = reducePlayback(
            applied,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                attachedOperationId = 2L,
                controllerLooping = false,
                controllerVolume = 1.0f,
                controllerPlayWhenReady = false,
            ),
        )

        val load = connected.sideEffects
            .filterIsInstance<PlaybackSideEffect.LoadSource>()
            .single()
        assertEquals(2L, connected.state.pending.operationId)
        assertEquals(2L, connected.state.pending.pendingSourceOperationId)
        assertEquals(2L, load.operationId)
    }

    @Test
    fun controllerConnectedKeepsAppliedOperationWhenOwnerAndIdMatch() {
        val pending = reducePlayback(
            playbackState(operationId = 1L),
            PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = false)),
        )
        val applied = reducePlayback(
            pending.state,
            PlaybackMessage.EngineSourceLoaded(
                operationId = pending.state.pending.operationId,
                source = sourceA,
            ),
        ).state

        val connected = reducePlayback(
            applied,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                attachedOperationId = 2L,
                attachedOperationOwnedByClient = true,
                controllerLooping = false,
                controllerVolume = 1.0f,
                controllerPlayWhenReady = false,
            ),
        )

        assertTrue(
            connected.sideEffects.none { it is PlaybackSideEffect.LoadSource },
        )
        assertEquals(2L, connected.state.pending.operationId)
        assertNull(connected.state.pending.pendingSourceOperationId)
    }

    @Test
    fun controllerConnectedAdoptsAttachedSourceOperationId() {
        val state = playbackState(
            operationId = 1L,
        )
        val connected = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                attachedOperationId = 7L,
                controllerLooping = false,
                controllerVolume = 1.0f,
                controllerPlayWhenReady = false,
            ),
        )
        val error = PlaybackError(PlaybackErrorCode.PlaybackFailed, "Current operation failed.")

        val failed = reducePlayback(
            connected.state,
            PlaybackMessage.EngineFailed(operationId = 7L, error = error),
        )

        assertEquals(7L, connected.state.pending.operationId)
        assertEquals(error, failed.state.observed.error)
    }

    @Test
    fun controllerConnectedPushesUserOverrides() {
        val state = playbackState(
            request = PlaybackRequest(sourceA),
            pendingLooping = true,
            pendingVolume = 0.3f,
            pendingPlayWhenReady = false,
        )
        val result = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                controllerLooping = false,
                controllerVolume = 1.0f,
                controllerPlayWhenReady = true,
            ),
        )

        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.SetLooping && it.enabled })
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.SetVolume && it.volume == 0.3f })
        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.Pause })
    }

    @Test
    fun controllerConnectedAcknowledgesOverridesAlreadyAppliedByService() {
        val state = playbackState(
            request = PlaybackRequest(sourceA, autoplay = true, loopMode = LoopMode.One, volume = 0.3f),
            source = null,
            playRequested = true,
            isLooping = true,
            volume = 0.3f,
            pendingPlayWhenReady = true,
            pendingLooping = true,
            pendingVolume = 0.3f,
        )

        val result = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                controllerLooping = true,
                controllerVolume = 0.3f,
                controllerPlayWhenReady = true,
            ),
        )

        assertNull(result.state.pending.pendingPlayWhenReady)
        assertNull(result.state.pending.pendingLooping)
        assertNull(result.state.pending.pendingVolume)
        assertFalse(result.sideEffects.any { it is PlaybackSideEffect.Play })
        assertFalse(result.sideEffects.any { it is PlaybackSideEffect.SetLooping })
        assertFalse(result.sideEffects.any { it is PlaybackSideEffect.SetVolume })
    }

    @Test
    fun controllerConnectedAppliesRewindRequest() {
        val state = playbackState(
            request = PlaybackRequest(sourceA),
            rewindRequested = true,
            pendingPlayWhenReady = false,
            sleepTimerLifecycle = SleepTimerLifecycle.Cancelled,
        )
        val result = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = sourceA,
                controllerLooping = false,
                controllerVolume = 1.0f,
                controllerPlayWhenReady = true,
            ),
        )

        assertTrue(result.sideEffects.any { it is PlaybackSideEffect.Stop })
        assertFalse(result.sideEffects.any { it is PlaybackSideEffect.Play })
    }

    @Test
    fun controllerConnectedReleasesIfAlreadyReleased() {
        val state = PlaybackState(released = true)
        val result = reducePlayback(
            state,
            PlaybackMessage.ControllerConnected(
                attachedSource = null,
                controllerLooping = false,
                controllerVolume = 1.0f,
                controllerPlayWhenReady = false,
            ),
        )

        assertIs<PlaybackSideEffect.Release>(result.sideEffects.single())
    }

    @Test
    fun controllerDisconnectedClearsSource() {
        val state = playbackState(source = sourceA)
        val result = reducePlayback(state, PlaybackMessage.ControllerDisconnected)

        assertNull(result.state.observed.source)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun controllerConnectionFailedSetsServiceUnavailableError() {
        val state = playbackState(request = PlaybackRequest(sourceA))
        val result = reducePlayback(state, PlaybackMessage.ControllerConnectionFailed)

        assertEquals(PlaybackErrorCode.ServiceUnavailable, result.state.observed.error?.code)
        assertNull(result.state.observed.source)
        // The timer intent is left untouched (here: never set), not forced to Cancelled.
        assertIs<SleepTimerLifecycle.Unchanged>(result.state.sleepTimerLifecycle)
        assertTrue(result.sideEffects.isEmpty())
    }

    @Test
    fun controllerConnectionFailurePreservesRunningSleepTimer() {
        val state = playbackState(
            request = PlaybackRequest(sourceA),
            sleepTimerLifecycle = SleepTimerLifecycle.Running(90_000L),
        )
        val result = reducePlayback(state, PlaybackMessage.ControllerConnectionFailed)

        // A still-running service timer must survive a transient connection failure instead of
        // being cancelled on the next reconnect.
        assertEquals(SleepTimerLifecycle.Running(90_000L), result.state.sleepTimerLifecycle)
    }

    @Test
    fun controllerConnectionFailedWithNoRequestSetsNoError() {
        val result = reducePlayback(PlaybackState(), PlaybackMessage.ControllerConnectionFailed)
        assertNull(result.state.observed.error)
    }

    // ── iOS phase helper ─────────────────────────────────────────────────

    @Test
    fun playbackPhaseComputesCanonicalPhasesForBothPlatforms() {
        assertEquals(
            PlaybackPhase.Failed,
            playbackPhase(PlaybackError(PlaybackErrorCode.PlaybackFailed), false, true, true, false),
        )
        assertEquals(
            PlaybackPhase.Idle,
            playbackPhase(null, false, false, false, false),
        )
        assertEquals(
            PlaybackPhase.Ended,
            playbackPhase(null, true, true, true, false),
        )
        assertEquals(
            PlaybackPhase.Loading,
            playbackPhase(null, false, true, false, false),
        )
        assertEquals(
            PlaybackPhase.Buffering,
            playbackPhase(null, false, true, true, true),
        )
        assertEquals(
            PlaybackPhase.Ready,
            playbackPhase(null, false, true, true, false),
        )
    }

    // ── sleep timer deadline math ────────────────────────────────────────

    @Test
    fun remainingDurationUntilReturnsPositiveOrNull() {
        assertEquals(15_000L, remainingDurationUntil(20_000L, 5_000L))
        assertEquals(2_000L, remainingDurationUntil(20_000L, 18_000L))
        assertNull(remainingDurationUntil(20_000L, 20_000L))
        assertNull(remainingDurationUntil(20_000L, 25_000L))
        assertNull(remainingDurationUntil(Long.MIN_VALUE, 1_000L))
        assertNull(remainingDurationUntil(20_000L, -1L))
        assertEquals(Long.MAX_VALUE, remainingDurationUntil(Long.MAX_VALUE, 0L))
    }

    @Test
    fun sleepTimerDeadlineRejectsUnusableInput() {
        assertEquals(1_500L, sleepTimerDeadline(1_000L, 500L))
        assertFailsWith<IllegalArgumentException> { sleepTimerDeadline(1_000L, 0L) }
        assertFailsWith<IllegalArgumentException> { sleepTimerDeadline(Long.MAX_VALUE - 5L, 10L) }
    }
}
