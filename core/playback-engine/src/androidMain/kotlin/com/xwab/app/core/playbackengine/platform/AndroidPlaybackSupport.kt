package com.xwab.app.core.playbackengine.platform

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.xwab.app.core.playbackengine.api.PlaybackError
import com.xwab.app.core.playbackengine.api.PlaybackErrorCode
import com.xwab.app.core.playbackengine.api.PlaybackPhase
import com.xwab.app.core.playbackengine.store.playbackPhase

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
    error = PlaybackError(PlaybackErrorCode.PlaybackFailed).takeIf { hasError },
    ended = playbackState == Player.STATE_ENDED,
    hasCurrentItem = hasSource || hasRequestedSource,
    isReadyToPlay = hasSource && !isLoadPending,
    isWaitingToPlay = playbackState == Player.STATE_BUFFERING,
)
