package com.xwab.app.core.playback.platform

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.MediaSession
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Where the application states the user agent its playback should present.
 *
 * A manifest key rather than a constructor argument, because Android builds the service. The value
 * belongs to the app: `androidApp` declares it, and it has to agree with the identity content
 * owners attach to delivery requests, since both identify the same client to the same host.
 */
private const val USER_AGENT_METADATA_KEY = "com.xwab.app.core.playback.USER_AGENT"

/** The lifetime of [PlaybackServiceGraph]: one service instance, from `onCreate` to `onDestroy`. */
internal object PlaybackServiceScope

/**
 * The playback service's own graph, built in [PlaybackService.onCreate]. It builds the player, its
 * media session and the sleep timer; the service keeps them and releases them in `onDestroy`, as
 * Media3 has it.
 *
 * The service comes in as the [Context] and hands over its own session callback, which answers
 * from the service's state.
 */
@DependencyGraph(PlaybackServiceScope::class, bindingContainers = [AndroidPlaybackBindings::class])
internal interface PlaybackServiceGraph {
    val player: ExoPlayer
    val mediaSession: MediaSession
    val sleepTimerFactory: SleepTimer.Factory

    /**
     * How this app identifies itself to a host it streams from, or null when it does not say.
     *
     * Read at all because the header the app attaches to its *downloads* never reaches this
     * player: a sound that is not cached yet is opened here, directly, and until this the request
     * went out under whatever the platform's HTTP stack calls itself. Some hosts refuse that.
     */
    @Provides
    @ApplicationUserAgent
    fun provideUserAgent(context: Context): String? {
        val packageManager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        }
        return info.metaData?.getString(USER_AGENT_METADATA_KEY)?.takeIf { it.isNotBlank() }
    }

    /**
     * ExoPlayer's default source chain with the user agent on its HTTPS half.
     *
     * [DefaultDataSource.Factory] is what keeps local playback working: a cached sound resolves to
     * a file path. With no user agent this is exactly the chain `ExoPlayer.Builder` builds by
     * default, since a null user agent leaves the platform's own in place.
     */
    @OptIn(UnstableApi::class)
    @Provides
    fun provideMediaSourceFactory(
        context: Context,
        @ApplicationUserAgent userAgent: String?,
    ): MediaSource.Factory = DefaultMediaSourceFactory(
        DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory().setUserAgent(userAgent),
        ),
    )

    @Provides
    @SingleIn(PlaybackServiceScope::class)
    fun providePlayer(context: Context, mediaSourceFactory: MediaSource.Factory): ExoPlayer =
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

    /** Opens the app from the media notification, when the app has a launcher activity. */
    @Provides
    @SingleIn(PlaybackServiceScope::class)
    fun provideMediaSession(
        context: Context,
        player: ExoPlayer,
        callback: MediaSession.Callback,
    ): MediaSession {
        val builder = MediaSession.Builder(context, player).setCallback(callback)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launchIntent ->
            builder.setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        return builder.build()
    }

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
            @Provides callback: MediaSession.Callback,
        ): PlaybackServiceGraph
    }
}
