package com.xwab.app.core.playback.platform

import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import co.touchlab.kermit.Logger
import com.google.common.util.concurrent.ListenableFuture
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import java.util.concurrent.Executor

/**
 * Owns the Android MediaSession controller connection lifecycle. It starts connecting as soon as
 * it is created.
 */
@AssistedInject
internal class MediaControllerConnection(
    context: Context,
    private val sessionToken: SessionToken,
    private val mainExecutor: Executor,
    @Assisted private val onConnected: (MediaController) -> Unit,
    @Assisted private val onControllerDisconnected: (MediaController) -> Unit,
    @Assisted private val onConnectionFailed: () -> Unit,
) : MediaController.Listener {
    /** The callbacks are the owner's; everything else comes from the module graph. */
    @AssistedFactory
    fun interface Factory {
        fun create(
            onConnected: (MediaController) -> Unit,
            onControllerDisconnected: (MediaController) -> Unit,
            onConnectionFailed: () -> Unit,
        ): MediaControllerConnection
    }

    private val appContext = context.applicationContext
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
            mainExecutor,
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
