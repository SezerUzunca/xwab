package com.xwab.app.core.playbackengine.platform

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import co.touchlab.kermit.Logger
import com.google.common.util.concurrent.ListenableFuture

/** Owns the Android MediaSession controller connection lifecycle. */
internal class MediaControllerConnection(
    context: Context,
    private val onConnected: (MediaController) -> Unit,
    private val onControllerDisconnected: (MediaController) -> Unit,
    private val onConnectionFailed: () -> Unit,
) : MediaController.Listener {
    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(
        appContext,
        ComponentName(appContext, PlaybackService::class.java),
    )
    private val logger = Logger.withTag("MediaControllerConnection")

    private var pendingConnectionFuture: ListenableFuture<MediaController>? = null
    private var activeMediaController: MediaController? = null
    private var released = false

    val currentController: MediaController?
        get() = activeMediaController

    init {
        connect()
    }

    fun connect() {
        if (released || activeMediaController != null || pendingConnectionFuture != null) return

        val future = MediaController.Builder(appContext, sessionToken)
            .setListener(this)
            .buildAsync()
        pendingConnectionFuture = future
        future.addListener(
            { completeConnection(future) },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun release() {
        if (released) return
        released = true
        activeMediaController?.release()
        activeMediaController = null
        pendingConnectionFuture?.let(MediaController::releaseFuture)
        pendingConnectionFuture = null
    }

    override fun onDisconnected(controller: MediaController) {
        if (released || this.activeMediaController !== controller) return
        this.activeMediaController = null
        pendingConnectionFuture = null
        onControllerDisconnected(controller)
    }

    private fun completeConnection(future: ListenableFuture<MediaController>) {
        if (released || pendingConnectionFuture !== future) return

        val connectedController = try {
            future.get()
        } catch (error: Exception) {
            logger.e(error) { "Unable to connect to the playback service." }
            pendingConnectionFuture = null
            onConnectionFailed()
            return
        }

        pendingConnectionFuture = null
        activeMediaController = connectedController
        onConnected(connectedController)
    }
}
