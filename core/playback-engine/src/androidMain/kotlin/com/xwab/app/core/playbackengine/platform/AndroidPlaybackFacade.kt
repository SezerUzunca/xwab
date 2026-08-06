package com.xwab.app.core.playbackengine.platform

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.xwab.app.core.playbackengine.api.AudioPlayerState
import com.xwab.app.core.playbackengine.api.AudioSource
import com.xwab.app.core.playbackengine.api.PlaybackCommand
import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbackengine.api.PlaybackError
import com.xwab.app.core.playbackengine.api.PlaybackErrorCode
import com.xwab.app.core.playbackengine.api.PlaybackPhase
import com.xwab.app.core.playbackengine.api.PlaybackRequest
import com.xwab.app.core.playbackengine.api.SleepTimerState
import com.xwab.app.core.playbackengine.projection.projectPlaybackState
import com.xwab.app.core.playbackengine.store.PLAYBACK_READINESS_TIMEOUT_MS
import com.xwab.app.core.playbackengine.store.PlaybackMessage
import com.xwab.app.core.playbackengine.store.PlaybackSideEffect
import com.xwab.app.core.playbackengine.store.PlaybackState
import com.xwab.app.core.playbackengine.store.PlaybackStore
import com.xwab.app.core.playbackengine.store.remainingDurationUntil
import com.xwab.app.core.playbackengine.store.sleepTimerDeadline
import com.xwab.app.core.playbackengine.store.toMessage
import com.xwab.app.core.playbackengine.timer.SleepTimerTicker
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun createAndroidPlaybackController(context: Context): PlaybackController {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "createAndroidPlaybackController must be called from the main thread."
    }
    return AndroidPlaybackFacade(context)
}

private class AndroidPlaybackFacade(
    context: Context
) : PlaybackController, Player.Listener {

    private val _state = MutableStateFlow(AudioPlayerState())
    override val state: StateFlow<AudioPlayerState> = _state.asStateFlow()
    private val sleepTimerTicker = SleepTimerTicker(
        // The service owns the timer, so the countdown must read the same
        // monotonic clock the deadlines are expressed in.
        nowMs = SystemClock::elapsedRealtime,
        scheduler = HandlerTickScheduler(),
        // PlaybackService is the sole Android expiry authority.  The local
        // ticker only clears the UI countdown when the same deadline passes;
        // it must not issue a second pause/seek through the controller.
        onExpired = { dispatch(PlaybackMessage.SleepTimerDeadlineObserved(null)) },
    )
    private val sleepTimerClient = SleepTimerClient(
        mainExecutor = ContextCompat.getMainExecutor(context.applicationContext),
    )
    override val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerTicker.state

    private val store = PlaybackStore(::executeEffects, ::publishState)
    private val playbackState: PlaybackState get() = store.state
    private val operationOwnerId = UUID.randomUUID().toString()
    private val loadTimeoutScheduler = HandlerTickScheduler()
    private var pendingLoad: PendingLoad? = null

    private val connection = MediaControllerConnection(
        context = context.applicationContext,
        onConnected = ::onControllerConnected,
        onControllerDisconnected = ::onControllerDisconnected,
        onConnectionFailed = ::onControllerConnectionFailed,
    )

    private fun dispatch(intent: PlaybackMessage) = store.dispatch(intent)

    override fun submit(command: PlaybackCommand) {
        checkMainThread()
        if (playbackState.released) return
        // Only the timer commands need Android's service-owned deadline handling;
        // everything else uses the shared mapping.
        when (command) {
            is PlaybackCommand.StartSleepTimer -> {
                val deadlineElapsedRealtimeMs = sleepTimerDeadline(
                    nowMs = SystemClock.elapsedRealtime(),
                    durationMs = command.durationMs,
                )
                sleepTimerTicker.applyDeadline(deadlineElapsedRealtimeMs)
                dispatch(command.toMessage(deadlineElapsedRealtimeMs))
            }
            PlaybackCommand.CancelSleepTimer -> {
                // Invalidate older IPC responses, but keep the last confirmed countdown
                // visible until the service acknowledges cancellation.
                sleepTimerClient.clear()
                dispatch(PlaybackMessage.CancelSleepTimer)
            }
            else -> dispatch(command.toMessage())
        }
    }

    override fun release() {
        checkMainThread()
        if (playbackState.released) return
        // Teardown itself lives in the Release effect, so it also runs when the
        // reducer releases for any other reason (matching iOS).
        dispatch(PlaybackMessage.Release)
    }

    private fun executeEffects(effects: List<PlaybackSideEffect>) {
        for (effect in effects) {
            when (effect) {
                is PlaybackSideEffect.LoadSource -> ensureConnectedOrApply { controller ->
                    applyLoad(
                        controller = controller,
                        operationId = effect.operationId,
                        request = effect.request,
                        isLooping = effect.isLooping,
                    )
                }
                is PlaybackSideEffect.Play -> ensureConnectedOrApply { controller -> applyPlay(controller) }
                is PlaybackSideEffect.Pause -> ensureConnectedOrApply { controller -> controller.pause() }
                // Android's audio-session interruptions are handled natively by the
                // media session; a soft pause behaves like a normal pause here.
                is PlaybackSideEffect.PauseForInterruption -> ensureConnectedOrApply { controller -> controller.pause() }
                is PlaybackSideEffect.SeekToStartThenPlay -> ensureConnectedOrApply { controller ->
                    controller.seekTo(0L)
                    controller.play()
                }
                is PlaybackSideEffect.SetLooping -> ensureConnectedOrApply { controller ->
                    controller.repeatMode = if (effect.enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                }
                is PlaybackSideEffect.SetVolume -> ensureConnectedOrApply { controller -> controller.volume = effect.volume }
                PlaybackSideEffect.Stop -> ensureConnectedOrApply { controller ->
                    controller.pause()
                    controller.seekTo(0L)
                }
                is PlaybackSideEffect.Release -> {
                    clearPendingLoad()
                    loadTimeoutScheduler.release()
                    sleepTimerTicker.release()
                    sleepTimerClient.clear()
                    _state.value = AudioPlayerState()
                    // Non-destructive teardown: disconnect this client only. The PlaybackService,
                    // its active playback, and its sleep timer keep running independently of the
                    // app's DI/container lifecycle, so background playback is NOT torn down here.
                    connection.currentController?.removeListener(this)
                    connection.release()
                }
                is PlaybackSideEffect.StartSleepTimer -> ensureConnectedOrApply { controller -> applyStartSleepTimer(controller, effect.deadlineElapsedRealMs) }
                is PlaybackSideEffect.CancelSleepTimer -> ensureConnectedOrApply { controller -> applyCancelSleepTimer(controller) }
                is PlaybackSideEffect.RestoreSleepTimer -> ensureConnectedOrApply { controller ->
                    reconcileSleepTimer(controller)
                }
                is PlaybackSideEffect.ReconnectSleepTimer -> ensureConnectedOrApply { controller ->
                    if (remainingDurationUntil(effect.deadlineElapsedRealMs, SystemClock.elapsedRealtime()) != null) {
                        applyStartSleepTimer(controller, effect.deadlineElapsedRealMs)
                    } else {
                        dispatch(PlaybackMessage.SleepTimerExpired)
                    }
                }
            }
        }
    }

    private fun applyLoad(
        controller: MediaController,
        operationId: Long,
        request: PlaybackRequest,
        isLooping: Boolean,
    ) {
        val requestedSource = request.source
        val metadata = MediaMetadata.Builder()
            .apply {
                requestedSource.title?.let(::setTitle)
                requestedSource.artist?.let(::setArtist)
            }
            .setExtras(
                Bundle().apply {
                    putLong(OPERATION_ID_METADATA_KEY, operationId)
                    putString(OPERATION_OWNER_METADATA_KEY, operationOwnerId)
                },
            )
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(requestedSource.id)
            .setUri(requestedSource.uri)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(requestedSource.uri.toUri())
                    .build(),
            )
            .build()

        beginPendingLoad(operationId, requestedSource)
        controller.pause()
        controller.repeatMode = if (isLooping) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
        controller.volume = playbackState.pending.pendingVolume ?: playbackState.desired.volume
        controller.setMediaItem(mediaItem)
        controller.prepare()
    }

    private fun applyPlay(controller: MediaController) {
        if (controller.playbackState == Player.STATE_ENDED) {
            controller.seekTo(0L)
        }
        if (controller.playbackState == Player.STATE_IDLE) {
            controller.prepare()
        }
        controller.play()
    }

    private fun applyStartSleepTimer(controller: MediaController, deadlineElapsedRealtimeMs: Long) {
        sleepTimerClient.start(
            controller = controller,
            deadlineElapsedRealtimeMs = deadlineElapsedRealtimeMs,
            onSuccess = ::applyObservedSleepTimerDeadline,
            onError = { reconcileSleepTimer(controller) },
        )
    }

    private fun applyCancelSleepTimer(controller: MediaController) {
        sleepTimerClient.cancel(
            controller = controller,
            onSuccess = ::applyObservedSleepTimerDeadline,
            onError = { reconcileSleepTimer(controller) },
        )
    }

    override fun onEvents(player: Player, events: Player.Events) {
        dispatch(
            PlaybackMessage.EnginePlaybackObserved(
                operationId = currentOperationId(player),
                source = readSource(player),
                playWhenReady = player.playWhenReady,
                looping = player.repeatMode == Player.REPEAT_MODE_ONE,
                volume = player.volume,
            ),
        )
        if (player.playbackState == Player.STATE_ENDED) {
            dispatch(PlaybackMessage.EnginePlaybackEnded(currentOperationId(player)))
        }
        pendingLoad
            ?.takeIf { it.source == readSource(player) && player.playbackState == Player.STATE_READY }
            ?.let { loaded ->
                clearPendingLoad()
                dispatch(PlaybackMessage.EngineSourceLoaded(loaded.operationId, loaded.source))
            }
    }

    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        val load = pendingLoad?.takeIf { it.source == connection.currentController?.let(::readSource) }
        if (load != null) clearPendingLoad()
        dispatch(
            if (load != null) PlaybackMessage.EngineFailed(
                operationId = load.operationId,
                error = PlaybackError(PlaybackErrorCode.InvalidSource, error.message),
            ) else PlaybackMessage.EngineFailed(
                operationId = connection.currentController?.let(::currentOperationId)
                    ?: playbackState.pending.operationId,
                error = PlaybackError(PlaybackErrorCode.PlaybackFailed, error.message),
            ),
        )
    }

    private fun onControllerConnected(controller: MediaController) {
        controller.addListener(this)
        dispatch(PlaybackMessage.ControllerConnected(
            attachedSource = readSource(controller),
            attachedOperationId = readAttachedOperationId(controller),
            attachedOperationOwnedByClient = isAttachedOperationOwnedByClient(controller),
            controllerLooping = controller.repeatMode == Player.REPEAT_MODE_ONE,
            controllerVolume = controller.volume,
            controllerPlayWhenReady = controller.playWhenReady,
        ))
    }

    private fun onControllerDisconnected(controller: MediaController) {
        clearPendingLoad()
        controller.removeListener(this)
        if (playbackState.released) {
            connection.release()
            return
        }
        dispatch(PlaybackMessage.ControllerDisconnected)
        connection.connect()
    }

    private fun onControllerConnectionFailed() {
        if (playbackState.released) {
            connection.release()
            return
        }
        clearSleepTimerState()
        dispatch(PlaybackMessage.ControllerConnectionFailed)
    }

    private fun clearSleepTimerState() {
        sleepTimerTicker.clear()
        sleepTimerClient.clear()
    }

    private fun applyObservedSleepTimerDeadline(deadlineElapsedRealtimeMs: Long?) {
        val activeDeadline = deadlineElapsedRealtimeMs?.takeIf {
            remainingDurationUntil(it, SystemClock.elapsedRealtime()) != null
        }
        sleepTimerTicker.applyDeadline(activeDeadline)
        dispatch(PlaybackMessage.SleepTimerDeadlineObserved(activeDeadline))
    }

    private fun reconcileSleepTimer(controller: MediaController) {
        sleepTimerClient.restore(
            controller = controller,
            onSuccess = ::applyObservedSleepTimerDeadline,
            onError = {
                // Preserve the last visible countdown. With no authoritative reply,
                // clearing it would falsely claim that the service timer is gone.
            },
        )
    }

    private fun beginPendingLoad(operationId: Long, source: AudioSource) {
        pendingLoad = PendingLoad(operationId, source)
        loadTimeoutScheduler.schedule(PLAYBACK_READINESS_TIMEOUT_MS) {
            val timedOut = pendingLoad?.takeIf {
                it.operationId == operationId && it.source == source
            } ?: return@schedule
            clearPendingLoad()
            dispatch(
                PlaybackMessage.EngineFailed(
                    timedOut.operationId,
                    PlaybackError(
                        PlaybackErrorCode.Timeout,
                        "Audio source did not become ready in time.",
                    ),
                ),
            )
        }
    }

    private fun clearPendingLoad(): PendingLoad? {
        loadTimeoutScheduler.cancel()
        return pendingLoad.also { pendingLoad = null }
    }

    private fun readSource(player: Player): AudioSource? {
        val item = player.currentMediaItem ?: return null
        val mediaId = item.mediaId.takeIf(String::isNotBlank) ?: return null
        val uri = item.localConfiguration?.uri?.toString()
            ?.takeIf(String::isNotBlank)
            ?: item.requestMetadata.mediaUri?.toString()?.takeIf(String::isNotBlank)
            ?: return null

        return AudioSource(
            id = mediaId,
            uri = uri,
            title = item.mediaMetadata.title?.toString(),
            artist = item.mediaMetadata.artist?.toString(),
        )
    }

    private fun readAttachedOperationId(player: Player): Long? =
        player.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getLong(OPERATION_ID_METADATA_KEY, 0L)
            ?.takeIf { it > 0L }

    private fun isAttachedOperationOwnedByClient(player: Player): Boolean =
        player.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getString(OPERATION_OWNER_METADATA_KEY) == operationOwnerId

    private fun currentOperationId(player: Player): Long =
        readAttachedOperationId(player) ?: playbackState.pending.operationId

    private fun publishState() {
        if (playbackState.released) return
        val p = connection.currentController
        if (p == null) {
            // No controller to read from: fall back to the model's own values.
            _state.value = projectPlaybackState(
                state = playbackState,
                phase = when {
                    playbackState.observed.error != null -> PlaybackPhase.Failed
                    playbackState.desired.request != null -> PlaybackPhase.Loading
                    else -> PlaybackPhase.Idle
                },
                isPlaying = false,
                volume = playbackState.desired.volume,
                error = playbackState.observed.error,
            )
            return
        }
        val error = p.playerError
            ?.let { PlaybackError(PlaybackErrorCode.PlaybackFailed, it.message) }
            ?: playbackState.observed.error
        val phase = androidPlaybackPhase(
            hasError = error != null,
            isLoadPending = pendingLoad != null,
            playbackState = p.playbackState,
            hasSource = playbackState.observed.source != null,
            hasRequestedSource = playbackState.desired.request != null,
        )
        _state.value = projectPlaybackState(
            state = playbackState,
            phase = phase,
            isPlaying = p.isPlaying,
            volume = p.volume,
            error = error,
        )
    }

    private fun ensureConnectedOrApply(action: (MediaController) -> Unit) {
        connection.currentController?.let(action) ?: connection.connect()
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "AndroidPlaybackFacade must be accessed from the main thread."
        }
    }

    private companion object {
        const val OPERATION_ID_METADATA_KEY =
            "com.xwab.app.core.playbackengine.PLAYBACK_OPERATION_ID"
        const val OPERATION_OWNER_METADATA_KEY =
            "com.xwab.app.core.playbackengine.PLAYBACK_OPERATION_OWNER"
    }

    private data class PendingLoad(
        val operationId: Long,
        val source: AudioSource,
    )
}
