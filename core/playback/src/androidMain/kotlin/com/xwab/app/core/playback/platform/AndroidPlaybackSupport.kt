package com.xwab.app.core.playback.platform

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.xwab.app.core.playback.port.PlaybackError
import com.xwab.app.core.playback.port.PlaybackErrorCode
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.store.playbackPhase
import com.xwab.app.core.playback.store.PlaybackMessage

/**
 * Android-specific helper functions used by [PlaybackService] and the
 * Android playback driver.
 */

internal enum class AndroidControllerAccess {
    OwnPackage,
    TrustedExternal,
    Rejected,
}

internal fun androidControllerAccess(
    isOwnPackage: Boolean,
    isTrusted: Boolean,
): AndroidControllerAccess = when {
    isOwnPackage -> AndroidControllerAccess.OwnPackage
    isTrusted -> AndroidControllerAccess.TrustedExternal
    else -> AndroidControllerAccess.Rejected
}

internal fun trustedExternalTransportCommandIds(): Set<Int> = setOf(
    Player.COMMAND_PLAY_PAUSE,
    Player.COMMAND_PREPARE,
    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
    Player.COMMAND_SEEK_BACK,
    Player.COMMAND_SEEK_FORWARD,
    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
    Player.COMMAND_GET_TIMELINE,
    Player.COMMAND_GET_METADATA,
    Player.COMMAND_GET_AUDIO_ATTRIBUTES,
    Player.COMMAND_GET_VOLUME,
)

@OptIn(UnstableApi::class)
internal fun trustedExternalTransportCommands(): Player.Commands {
    val builder = Player.Commands.Builder()
    trustedExternalTransportCommandIds().forEach(builder::add)
    return builder.build()
}

internal fun androidPlaybackPhase(
    hasError: Boolean,
    isLoadPending: Boolean,
    playbackState: Int,
    hasSource: Boolean,
    hasRequestedSource: Boolean,
): PlaybackPhase = playbackPhase(
    error = if (hasError) PlaybackError(PlaybackErrorCode.PlaybackFailed) else null,
    ended = playbackState == Player.STATE_ENDED,
    hasCurrentItem = hasSource || hasRequestedSource,
    isReadyToPlay = hasSource && !isLoadPending,
    isWaitingToPlay = playbackState == Player.STATE_BUFFERING,
)

internal fun androidPlaybackError(
    reducerError: PlaybackError?,
    playerErrorMessage: String?,
): PlaybackError? = reducerError
    ?: playerErrorMessage?.let { PlaybackError(PlaybackErrorCode.PlaybackFailed, it) }

internal fun androidTerminalPlaybackMessage(
    playbackState: Int,
    operationId: Long,
    playerErrorMessage: String?,
): PlaybackMessage? = playerErrorMessage?.let {
    PlaybackMessage.EngineFailed(
        operationId,
        PlaybackError(PlaybackErrorCode.PlaybackFailed, it),
    )
} ?: if (playbackState == Player.STATE_ENDED) {
    PlaybackMessage.EnginePlaybackEnded(operationId)
} else {
    null
}
