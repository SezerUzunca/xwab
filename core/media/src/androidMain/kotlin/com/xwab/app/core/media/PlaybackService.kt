package com.xwab.app.core.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import co.touchlab.kermit.Logger
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val logger = Logger.withTag("PlaybackService")

    private val sleepTimer = SleepTimer(
        onExpired = {
            player?.run {
                pause()
                seekTo(0L)
            }
        },
    )

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

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
            },
        )

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val builder = MediaSession.Builder(this, player)
            .setCallback(SleepTimerSessionCallback())

        if (pendingIntent != null) {
            builder.setSessionActivity(pendingIntent)
        }

        mediaSession = builder.build()
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
        if (remainingDurationUntil(deadlineElapsedRealtimeMs, SystemClock.elapsedRealtime()) == null) {
            return SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
        }

        sleepTimer.startUntil(deadlineElapsedRealtimeMs)
        return sleepTimerResult()
    }

    private fun cancelSleepTimer(): SessionResult {
        sleepTimer.cancel()
        return sleepTimerResult()
    }

    private fun sleepTimerResult(): SessionResult = SessionResult(
        SessionResult.RESULT_SUCCESS,
        SleepTimerProtocol.stateArguments(sleepTimer.deadlineElapsedRealtimeMs),
    )

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

            val resultBuilder = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
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
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
            }

            val result = when (customCommand.customAction) {
                SleepTimerProtocol.ACTION_START -> startSleepTimer(
                    SleepTimerProtocol.requestedDeadlineFrom(arguments),
                )
                SleepTimerProtocol.ACTION_CANCEL -> cancelSleepTimer()
                SleepTimerProtocol.ACTION_GET_STATE -> sleepTimerResult()
                else -> {
                    logger.w { "Unsupported custom action: ${customCommand.customAction}" }
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
                }
            }
            return Futures.immediateFuture(result)
        }
    }
}

private class SleepTimer(
    private val onExpired: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    var deadlineElapsedRealtimeMs: Long? = null
        private set

    private val expiration = object : Runnable {
        override fun run() {
            val deadline = deadlineElapsedRealtimeMs ?: return

            val remainingMs = deadline - SystemClock.elapsedRealtime()

            if (remainingMs > 0L) {
                handler.postDelayed(this, remainingMs)
                return
            }

            deadlineElapsedRealtimeMs = null
            onExpired()
        }
    }

    fun startUntil(deadlineElapsedRealtimeMs: Long) {
        val remainingMs = remainingDurationUntil(
            deadlineElapsedRealtimeMs,
            SystemClock.elapsedRealtime(),
        ) ?: 0L
        this.deadlineElapsedRealtimeMs = deadlineElapsedRealtimeMs
        handler.removeCallbacks(expiration)
        handler.postDelayed(expiration, remainingMs)
    }

    fun cancel() {
        handler.removeCallbacks(expiration)
        deadlineElapsedRealtimeMs = null
    }
}
