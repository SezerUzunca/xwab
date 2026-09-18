package com.xwab.app.core.playback.platform

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import co.touchlab.kermit.Logger
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.xwab.app.core.playback.store.remainingDurationUntil

/**
 * Where the application states the user agent its playback should present.
 *
 * A manifest key rather than a constructor argument, because Android builds the service. The value
 * belongs to the app: `androidApp` declares it, and it has to agree with the one `:core:sources`
 * attaches to its download requests, since both identify the same client to the same host.
 */
private const val USER_AGENT_METADATA_KEY = "com.xwab.app.core.playback.USER_AGENT"

internal class PlaybackService : MediaSessionService() {

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
            .apply { applicationUserAgent()?.let { setMediaSourceFactory(identifyingSources(it)) } }
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

    /**
     * How this app identifies itself to a host it streams from, or null when it does not say.
     *
     * Read from the application's manifest rather than injected, because Android constructs this
     * service and nothing can hand it a value. Read at all because the header the app attaches to
     * its *downloads* never reaches this player: a sound that is not cached yet is opened here,
     * directly, and until this the request went out under whatever the platform's HTTP stack calls
     * itself. Some hosts refuse that.
     *
     * It is a string the application owns. This module learns that requests should say who is
     * making them — which is a property of any HTTP client — and nothing about who that is.
     */
    private fun applicationUserAgent(): String? {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }
        return info.metaData?.getString(USER_AGENT_METADATA_KEY)?.takeIf { it.isNotBlank() }
    }

    /**
     * The default factory, with one thing changed.
     *
     * [DefaultDataSource.Factory] is what keeps local playback working: a cached sound resolves to
     * a file path, and only the HTTPS half of the chain is given the user agent.
     */
    @OptIn(UnstableApi::class)
    private fun identifyingSources(userAgent: String): MediaSource.Factory =
        DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                this,
                DefaultHttpDataSource.Factory().setUserAgent(userAgent),
            ),
        )

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
            reconcileDeadline()
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

    /**
     * Re-arms the uptime-based Handler from the elapsed-realtime deadline, or expires immediately.
     * This is called whenever playback resumes and whenever a controller reads timer state.
     */
    fun reconcileDeadline() {
        val deadline = deadlineElapsedRealtimeMs ?: return
        val remainingMs = remainingDurationUntil(deadline, SystemClock.elapsedRealtime())
        handler.removeCallbacks(expiration)
        if (remainingMs == null) {
            deadlineElapsedRealtimeMs = null
            onExpired()
        } else {
            handler.postDelayed(expiration, remainingMs)
        }
    }

    fun cancel() {
        handler.removeCallbacks(expiration)
        deadlineElapsedRealtimeMs = null
    }
}
