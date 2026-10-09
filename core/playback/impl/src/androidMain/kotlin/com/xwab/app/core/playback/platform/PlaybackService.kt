package com.xwab.app.core.playback.platform

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import co.touchlab.kermit.Logger
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.xwab.app.core.playback.store.remainingDurationUntil
import com.xwab.app.core.playback.timer.SLEEP_TIMER_FADE_MS
import com.xwab.app.core.playback.timer.SLEEP_TIMER_FADE_STEP_MS
import com.xwab.app.core.playback.timer.SleepTimerClock
import com.xwab.app.core.playback.timer.TickScheduler
import com.xwab.app.core.playback.timer.sleepTimerFadeVolume
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.createGraphFactory

internal class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val logger = Logger.withTag("PlaybackService")

    private lateinit var sleepTimer: SleepTimer

    override fun onCreate() {
        super.onCreate()

        // Android constructs the service, so it builds its graph here and keeps what it needs; the
        // graph itself is not kept.
        val graph = createGraphFactory<PlaybackServiceGraph.Factory>()
            .create(context = this, callback = SleepTimerSessionCallback())
        sleepTimer = graph.sleepTimerFactory.create(
            onExpired = {
                player?.run {
                    pause()
                    seekTo(0L)
                }
            },
            onFadeVolume = { volume -> player?.volume = volume },
        )
        val player = graph.player

        this.player = player

        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    logger.e(error) { "Playback service player failed." }
                    cancelSleepTimer()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        cancelSleepTimer()
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) {
                        // Handler delays use uptimeMillis and therefore stop advancing in deep
                        // sleep, while timer deadlines use elapsedRealtime. Reconcile before audio
                        // resumes so a deadline that passed while the device slept cannot play on.
                        sleepTimer.reconcileDeadline()
                    }
                }
            },
        )

        mediaSession = graph.mediaSession
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun isOwnController(controller: MediaSession.ControllerInfo): Boolean =
        controller.packageName == packageName && controller.uid == applicationInfo.uid

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (player?.playWhenReady == false || player?.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        cancelSleepTimer()
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun startSleepTimer(deadlineElapsedRealtimeMs: Long): SessionResult {
        if (!sleepTimer.startUntil(deadlineElapsedRealtimeMs)) {
            return SessionResult(SessionError.ERROR_BAD_VALUE)
        }
        return sleepTimerResult()
    }

    private fun cancelSleepTimer(): SessionResult {
        sleepTimer.cancel()
        return sleepTimerResult()
    }

    private fun sleepTimerResult(): SessionResult {
        // A controller reconnect is another opportunity to repair a Handler callback delayed by
        // deep sleep and return the actual service-owned state.
        sleepTimer.reconcileDeadline()
        return SessionResult(
            SessionResult.RESULT_SUCCESS,
            SleepTimerProtocol.stateArguments(sleepTimer.deadlineElapsedRealtimeMs),
        )
    }

    private inner class SleepTimerSessionCallback : MediaSession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val access = androidControllerAccess(
                isOwnPackage = isOwnController(controller),
                isTrusted = controller.isTrusted,
            )
            if (access == AndroidControllerAccess.Rejected) {
                logger.w { "Media controller connection rejected: ${controller.packageName}" }
                return MediaSession.ConnectionResult.reject()
            }

            // Starts empty: each accepted case below sets both command sets itself.
            val resultBuilder = MediaSession.ConnectionResult.AcceptedResultBuilder()
            when (access) {
                AndroidControllerAccess.OwnPackage -> {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .apply {
                            add(SleepTimerProtocol.command(SleepTimerProtocol.ACTION_START))
                            add(SleepTimerProtocol.command(SleepTimerProtocol.ACTION_CANCEL))
                            add(SleepTimerProtocol.command(SleepTimerProtocol.ACTION_GET_STATE))
                        }
                        .build()
                    resultBuilder
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(
                            MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
                        )
                }
                AndroidControllerAccess.TrustedExternal -> {
                    resultBuilder
                        .setAvailableSessionCommands(SessionCommands.EMPTY)
                        .setAvailablePlayerCommands(trustedExternalTransportCommands())
                }
                AndroidControllerAccess.Rejected -> error("Rejected controllers return above.")
            }
            return resultBuilder.build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            arguments: android.os.Bundle,
        ): ListenableFuture<SessionResult> {
            if (!isOwnController(controller)) {
                logger.w { "Custom command denied for untrusted package: ${controller.packageName}" }
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
            }

            val result = when (customCommand.customAction) {
                SleepTimerProtocol.ACTION_START -> startSleepTimer(
                    SleepTimerProtocol.requestedDeadlineFrom(arguments),
                )
                SleepTimerProtocol.ACTION_CANCEL -> cancelSleepTimer()
                SleepTimerProtocol.ACTION_GET_STATE -> sleepTimerResult()
                else -> {
                    logger.w { "Unsupported custom action: ${customCommand.customAction}" }
                    SessionResult(SessionError.ERROR_NOT_SUPPORTED)
                }
            }
            return Futures.immediateFuture(result)
        }
    }
}

/**
 * The service-owned sleep timer: stops playback at the deadline, fading it out over the last
 * [SLEEP_TIMER_FADE_MS] first.
 *
 * @param onFadeVolume receives the player volume while fading, and full volume again once the
 *   timer expires (after [onExpired] has paused, so it is never heard), is cancelled or is restarted.
 */
@AssistedInject
internal class SleepTimer(
    private val clock: SleepTimerClock,
    private val scheduler: TickScheduler,
    @Assisted private val onExpired: () -> Unit,
    @Assisted private val onFadeVolume: (Float) -> Unit,
) {
    /** The callbacks are the service's own; the clock and the scheduler come from its graph. */
    @AssistedFactory
    fun interface Factory {
        fun create(onExpired: () -> Unit, onFadeVolume: (Float) -> Unit): SleepTimer
    }

    private var appliedVolume = FULL_VOLUME

    var deadlineElapsedRealtimeMs: Long? = null
        private set

    /**
     * Runs the timer until [deadlineElapsedRealtimeMs]. Returns false and changes nothing when that
     * deadline has already passed or cannot be represented.
     */
    fun startUntil(deadlineElapsedRealtimeMs: Long): Boolean {
        if (remainingDurationUntil(deadlineElapsedRealtimeMs, clock.nowMs()) == null) return false
        this.deadlineElapsedRealtimeMs = deadlineElapsedRealtimeMs
        reconcileDeadline()
        return true
    }

    /**
     * Re-arms the uptime-based scheduler from the elapsed-realtime deadline, or expires immediately.
     * This is called whenever playback resumes, whenever a controller reads timer state, and on each
     * step of the fade.
     *
     * Before the fade window it sleeps until the window opens; inside it, it wakes every
     * [SLEEP_TIMER_FADE_STEP_MS] to lower the volume.
     */
    fun reconcileDeadline() {
        val deadline = deadlineElapsedRealtimeMs ?: return
        val remainingMs = remainingDurationUntil(deadline, clock.nowMs())
        scheduler.cancel()
        if (remainingMs == null) {
            deadlineElapsedRealtimeMs = null
            onExpired()
            applyVolume(FULL_VOLUME)
            return
        }
        applyVolume(sleepTimerFadeVolume(remainingMs))
        val untilFadeMs = remainingMs - SLEEP_TIMER_FADE_MS
        val nextTickMs = if (untilFadeMs > 0) untilFadeMs else remainingMs.coerceAtMost(SLEEP_TIMER_FADE_STEP_MS)
        scheduler.schedule(nextTickMs, ::reconcileDeadline)
    }

    fun cancel() {
        scheduler.cancel()
        deadlineElapsedRealtimeMs = null
        applyVolume(FULL_VOLUME)
    }

    private fun applyVolume(volume: Float) {
        if (volume == appliedVolume) return
        appliedVolume = volume
        onFadeVolume(volume)
    }

    private companion object {
        const val FULL_VOLUME = 1.0f
    }
}
