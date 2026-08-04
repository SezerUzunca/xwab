package com.xwab.app.core.media.platform

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import co.touchlab.kermit.Logger
import java.util.concurrent.Executor

/**
 * Handles low-level Media3 [MediaController] custom command IPC for the sleep timer.
 *
 * Encapsulates out-of-order response protection ([requestVersion]) and sends requests
 * to [PlaybackService].
 */
internal class SleepTimerClient(
    private val mainExecutor: Executor,
) {
    private val logger = Logger.withTag("AndroidSleepTimer")
    private var requestVersion = 0L

    fun start(
        controller: MediaController,
        deadlineElapsedRealtimeMs: Long,
        onSuccess: (Long?) -> Unit,
        onError: () -> Unit,
    ) {
        require(deadlineElapsedRealtimeMs > 0L) { "Sleep timer deadline must be positive." }
        val version = nextRequestVersion()
        sendCommand(
            controller = controller,
            action = SleepTimerProtocol.ACTION_START,
            arguments = SleepTimerProtocol.startArguments(deadlineElapsedRealtimeMs),
            version = version,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    fun cancel(
        controller: MediaController,
        onSuccess: (Long?) -> Unit,
        onError: () -> Unit,
    ) {
        val version = nextRequestVersion()
        sendCommand(
            controller = controller,
            action = SleepTimerProtocol.ACTION_CANCEL,
            arguments = Bundle.EMPTY,
            version = version,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    fun restore(
        controller: MediaController,
        onSuccess: (Long?) -> Unit,
        onError: () -> Unit,
    ) {
        val version = nextRequestVersion()
        sendCommand(
            controller = controller,
            action = SleepTimerProtocol.ACTION_GET_STATE,
            arguments = Bundle.EMPTY,
            version = version,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    fun clear() {
        nextRequestVersion()
    }

    private fun sendCommand(
        controller: MediaController,
        action: String,
        arguments: Bundle,
        version: Long,
        onSuccess: (Long?) -> Unit,
        onError: (() -> Unit)?,
    ) {
        val resultFuture = runCatching {
            controller.sendCustomCommand(
                SleepTimerProtocol.command(action),
                arguments,
            )
        }.getOrElse { error ->
            if (version == requestVersion) {
                logger.e(error) { "Unable to send sleep timer command: $action" }
                onError?.invoke()
            }
            return
        }
        resultFuture.addListener(
            {
                val result = runCatching(resultFuture::get).getOrElse { error ->
                    if (version == requestVersion) {
                        logger.e(error) { "Sleep timer command failed: $action" }
                        onError?.invoke()
                    }
                    return@addListener
                }
                if (version != requestVersion) return@addListener

                if (result.resultCode != SessionResult.RESULT_SUCCESS) {
                    logger.e { "Sleep timer command returned non-success code (${result.resultCode}): $action" }
                    onError?.invoke()
                    return@addListener
                }

                onSuccess(SleepTimerProtocol.deadlineFrom(result.extras))
            },
            mainExecutor,
        )
    }

    private fun nextRequestVersion(): Long {
        requestVersion += 1L
        return requestVersion
    }
}
