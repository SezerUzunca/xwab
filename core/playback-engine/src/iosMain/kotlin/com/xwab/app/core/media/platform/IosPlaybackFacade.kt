@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.xwab.app.core.media.platform

import co.touchlab.kermit.Logger
import com.xwab.app.core.media.api.AudioPlayerState
import com.xwab.app.core.media.api.AudioSource
import com.xwab.app.core.media.api.PlaybackCommand
import com.xwab.app.core.media.api.PlaybackController
import com.xwab.app.core.media.api.PlaybackError
import com.xwab.app.core.media.api.PlaybackErrorCode
import com.xwab.app.core.media.api.SleepTimerState
import com.xwab.app.core.media.projection.projectPlaybackState
import com.xwab.app.core.media.store.PlaybackMessage
import com.xwab.app.core.media.store.PlaybackSideEffect
import com.xwab.app.core.media.store.PlaybackState
import com.xwab.app.core.media.store.PlaybackStore
import com.xwab.app.core.media.store.playbackPhase
import com.xwab.app.core.media.store.sleepTimerDeadline
import com.xwab.app.core.media.store.toMessage
import com.xwab.app.core.media.timer.SleepTimerTicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSThread
import kotlin.time.TimeSource

fun createIosPlaybackController(): PlaybackController {
    check(NSThread.isMainThread) { "createIosPlaybackController must be called on the main thread." }
    return IosPlaybackFacade()
}

/**
 * A millisecond reading of a process-local monotonic clock.
 *
 * Only iOS needs this: it owns its sleep timer in-process, so it derives its own
 * deadlines. Android instead reads `SystemClock.elapsedRealtime`, because its
 * deadlines are shared with the PlaybackService and must use that timebase.
 */
private fun monotonicMillisSource(): () -> Long {
    val origin = TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeMilliseconds }
}

private class IosPlaybackFacade : PlaybackController {
    private val mutableState = MutableStateFlow(AudioPlayerState())
    override val state: StateFlow<AudioPlayerState> = mutableState.asStateFlow()
    private val nowMs: () -> Long = monotonicMillisSource()
    private val sleepTimer = SleepTimerTicker(
        nowMs = nowMs,
        scheduler = CoroutineTickScheduler(),
        onExpired = { dispatch(PlaybackMessage.SleepTimerExpired) },
    )
    override val sleepTimerState: StateFlow<SleepTimerState> = sleepTimer.state
    private val logger = Logger.withTag("IosPlaybackFacade")

    private val store = PlaybackStore(::executeEffects, ::publishState)
    private val playbackState: PlaybackState get() = store.state
    private var lastLoggedError: PlaybackError? = null
    private var pendingLoad: PendingLoad? = null

    private val engine: IosPlaybackEngine = IosPlaybackEngine(
        onStateChanged = { publishState() },
        onPlaybackEnded = { operationId ->
            dispatch(PlaybackMessage.EnginePlaybackEnded(operationId))
            publishState(forceNowPlayingUpdate = true)
        },
        onPlaybackFailed = { operationId, message ->
            clearPendingLoad(operationId)
            dispatch(
                PlaybackMessage.EngineFailed(
                    operationId,
                    PlaybackError(
                        PlaybackErrorCode.PlaybackFailed,
                        message ?: "Playback failed before reaching the end.",
                    ),
                ),
            )
        },
        onReadinessTimedOut = { operationId ->
            if (!playbackState.released && engine.hasCurrentItem) {
                clearPendingLoad(operationId)
                dispatch(
                    PlaybackMessage.EngineFailed(
                        operationId,
                        PlaybackError(
                            PlaybackErrorCode.Timeout,
                            "Audio source did not become ready in time.",
                        ),
                    ),
                )
            }
        },
    )
    private val mediaSession: AppleMediaSession = AppleMediaSession(
        onPlayRequested = { submit(PlaybackCommand.Play) },
        onPauseRequested = { submit(PlaybackCommand.Pause) },
        onToggleRequested = ::togglePlayback,
        onInterruptionBegan = {
            dispatch(PlaybackMessage.EngineInterrupted)
        },
    )
    private val nowPlayingInfoPublisher: NowPlayingInfoPublisher = NowPlayingInfoPublisher()

    private fun dispatch(intent: PlaybackMessage) = store.dispatch(intent)

    private fun executeEffects(effects: List<PlaybackSideEffect>) {
        for (effect in effects) {
            when (effect) {
                is PlaybackSideEffect.LoadSource -> {
                    mediaSession.clearInterruptionState()
                    mediaSession.deactivate()
                    engine.volume = effect.request.volume
                    pendingLoad = PendingLoad(effect.operationId, effect.request.source)
                    val ok = engine.load(
                        uri = effect.request.source.uri,
                        looping = effect.isLooping,
                        operationId = effect.operationId,
                    )
                    if (!ok) {
                        clearPendingLoad(effect.operationId)
                        dispatch(
                            PlaybackMessage.EngineFailed(
                                effect.operationId,
                                PlaybackError(
                                    PlaybackErrorCode.InvalidSource,
                                    "Invalid audio source URI.",
                                ),
                            ),
                        )
                    }
                }
                PlaybackSideEffect.Play -> {
                    if (!activatedForPlayback()) continue
                    engine.play()
                }
                PlaybackSideEffect.Pause -> {
                    engine.pause()
                    mediaSession.clearInterruptionState()
                    mediaSession.deactivate()
                }
                PlaybackSideEffect.PauseForInterruption -> {
                    engine.pause(notify = false)
                    mediaSession.deactivate()
                }
                PlaybackSideEffect.SeekToStartThenPlay -> {
                    if (!activatedForPlayback()) continue
                    engine.seekTo(0L) { finished ->
                        if (finished && playbackState.desired.playRequested) engine.play()
                        publishState(forceNowPlayingUpdate = true)
                    }
                }
                is PlaybackSideEffect.SetLooping -> {
                    if (engine.hasCurrentItem) {
                        engine.setLooping(effect.enabled, engine.currentPositionMs()) { finished ->
                            if (finished && playbackState.desired.playRequested) engine.play()
                            if (finished) {
                                publishState(forceNowPlayingUpdate = true)
                            }
                        }
                    }
                }
                is PlaybackSideEffect.SetVolume -> {
                    engine.volume = effect.volume
                }
                PlaybackSideEffect.Stop -> {
                    engine.stop { finished ->
                        if (finished) publishState(forceNowPlayingUpdate = true)
                    }
                    mediaSession.clearInterruptionState()
                    mediaSession.deactivate()
                }
                is PlaybackSideEffect.StartSleepTimer -> {
                    sleepTimer.applyDeadline(effect.deadlineElapsedRealMs)
                }
                PlaybackSideEffect.CancelSleepTimer -> {
                    sleepTimer.clear()
                }
                PlaybackSideEffect.Release -> {
                    pendingLoad = null
                    sleepTimer.release()
                    engine.release()
                    mediaSession.release()
                    nowPlayingInfoPublisher.release()
                    mutableState.value = AudioPlayerState()
                }
                is PlaybackSideEffect.ReconnectSleepTimer,
                PlaybackSideEffect.RestoreSleepTimer -> {
                    // Android-only or unused on iOS
                }
            }
        }
    }

    override fun submit(command: PlaybackCommand) = onPlayerThread {
        if (command is PlaybackCommand.StartSleepTimer) {
            val deadline = sleepTimerDeadline(nowMs(), command.durationMs)
            dispatch(command.toMessage(deadline))
        } else {
            dispatch(command.toMessage())
        }
    }

    override fun release() {
        checkPlayerThread()
        if (playbackState.released) return
        dispatch(PlaybackMessage.Release)
    }

    /**
     * Guards the two effects that start audio: both need a loaded item and an active
     * audio session, and both report the same failure when the session refuses.
     *
     * @return true when playback may proceed.
     */
    private fun activatedForPlayback(): Boolean {
        if (!engine.hasCurrentItem) return false
        if (mediaSession.activate()) return true

        dispatch(
            PlaybackMessage.EngineFailed(
                playbackState.pending.operationId,
                PlaybackError(
                    PlaybackErrorCode.PlaybackFailed,
                    "Failed to activate audio session.",
                ),
            ),
        )
        return false
    }

    private fun togglePlayback() {
        if (playbackState.desired.playRequested) {
            submit(PlaybackCommand.Pause)
        } else {
            submit(PlaybackCommand.Play)
        }
    }

    private fun publishState(forceNowPlayingUpdate: Boolean = false) {
        if (playbackState.released) return

        pendingLoad
            ?.takeIf { it.operationId == engine.currentOperationId && engine.isReadyToPlay }
            ?.let { loaded ->
                pendingLoad = null
                dispatch(PlaybackMessage.EngineSourceLoaded(loaded.operationId, loaded.source))
            }

        if (playbackState.observed.error != null) {
            engine.suspendObservationForFailure()
        }

        if (playbackState.observed.error == null) {
            engine.loopErrorMessage?.let { msg ->
                clearPendingLoad(engine.currentOperationId)
                dispatch(
                    PlaybackMessage.EngineFailed(
                        engine.currentOperationId,
                        PlaybackError(PlaybackErrorCode.PlaybackFailed, msg),
                    ),
                )
                return
            }
            if (engine.hasItemFailure) {
                clearPendingLoad(engine.currentOperationId)
                dispatch(
                    PlaybackMessage.EngineFailed(
                        engine.currentOperationId,
                        PlaybackError(
                            PlaybackErrorCode.PlaybackFailed,
                            engine.itemErrorMessage,
                        ),
                    ),
                )
                return
            }
        }

        logNewErrorIfNeeded()

        val phase = playbackPhase(
            error = playbackState.observed.error,
            ended = playbackState.desired.ended,
            hasCurrentItem = engine.hasCurrentItem,
            isReadyToPlay = engine.isReadyToPlay,
            isWaitingToPlay = engine.isWaitingToPlay,
        )
        val currentPositionMs = engine.currentPositionMs()
        val durationMs = engine.durationMs()
        val isPlaying = engine.isPlaying
        mediaSession.setCommandsEnabled(
            playbackState.observed.source != null && playbackState.observed.error == null,
        )

        mutableState.value = projectPlaybackState(
            state = playbackState,
            phase = phase,
            isPlaying = isPlaying,
            volume = engine.volume,
            error = playbackState.observed.error,
        )
        nowPlayingInfoPublisher.publish(
            source = playbackState.observed.source,
            phase = phase,
            durationMs = durationMs,
            positionMs = currentPositionMs,
            isPlaying = isPlaying,
            force = forceNowPlayingUpdate,
        )
    }

    private fun logNewErrorIfNeeded() {
        val error = playbackState.observed.error ?: run {
            lastLoggedError = null
            return
        }
        if (error == lastLoggedError) return

        lastLoggedError = error
        logger.e {
            "iOS playback failed [${error.code}]: ${error.message ?: "No error message provided."}"
        }
    }

    private inline fun onPlayerThread(action: () -> Unit) {
        checkPlayerThread()
        if (!playbackState.released) action()
    }

    private fun checkPlayerThread() {
        check(NSThread.isMainThread) {
            "IosPlaybackFacade must be accessed from the main thread."
        }
    }

    private fun clearPendingLoad(operationId: Long) {
        if (pendingLoad?.operationId == operationId) pendingLoad = null
    }

    private data class PendingLoad(
        val operationId: Long,
        val source: AudioSource,
    )
}
