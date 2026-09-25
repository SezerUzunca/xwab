package com.xwab.app.core.playback.platform

import androidx.media3.common.Player
import com.xwab.app.core.playback.port.PlaybackError
import com.xwab.app.core.playback.port.PlaybackErrorCode
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.store.PlaybackMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidPlaybackSupportTest {

    @Test
    fun nativePlayerStatesMapToCanonicalPhases() {
        assertEquals(PlaybackPhase.Failed, androidPlaybackPhase(true, false, Player.STATE_READY, true, true))
        assertEquals(PlaybackPhase.Loading, androidPlaybackPhase(false, true, Player.STATE_BUFFERING, true, true))
        assertEquals(PlaybackPhase.Buffering, androidPlaybackPhase(false, false, Player.STATE_BUFFERING, true, true))
        assertEquals(PlaybackPhase.Ready, androidPlaybackPhase(false, false, Player.STATE_READY, true, true))
        assertEquals(PlaybackPhase.Loading, androidPlaybackPhase(false, false, Player.STATE_READY, false, true))
        assertEquals(PlaybackPhase.Ended, androidPlaybackPhase(false, false, Player.STATE_ENDED, true, true))
        assertEquals(PlaybackPhase.Loading, androidPlaybackPhase(false, false, Player.STATE_IDLE, false, true))
        assertEquals(PlaybackPhase.Idle, androidPlaybackPhase(false, false, Player.STATE_IDLE, false, false))
    }

    @Test
    fun controllerAccessAllowsOwnAndTrustedControllersOnly() {
        assertEquals(
            AndroidControllerAccess.OwnPackage,
            androidControllerAccess(isOwnPackage = true, isTrusted = false),
        )
        assertEquals(
            AndroidControllerAccess.TrustedExternal,
            androidControllerAccess(isOwnPackage = false, isTrusted = true),
        )
        assertEquals(
            AndroidControllerAccess.Rejected,
            androidControllerAccess(isOwnPackage = false, isTrusted = false),
        )
    }

    @Test
    fun trustedExternalCommandsAreTransportOnly() {
        val commands = trustedExternalTransportCommandIds()

        assertTrue(Player.COMMAND_PLAY_PAUSE in commands)
        assertTrue(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM in commands)
        assertTrue(Player.COMMAND_GET_METADATA in commands)
        assertFalse(Player.COMMAND_STOP in commands)
        assertFalse(Player.COMMAND_SET_MEDIA_ITEM in commands)
        assertFalse(Player.COMMAND_CHANGE_MEDIA_ITEMS in commands)
        assertFalse(Player.COMMAND_SET_REPEAT_MODE in commands)
        assertFalse(Player.COMMAND_SET_VOLUME in commands)
    }

    @Test
    fun reducerErrorClassificationWinsOverTheNativeFallback() {
        val invalidSource = PlaybackError(PlaybackErrorCode.InvalidSource, "bad URI")

        assertEquals(
            invalidSource,
            androidPlaybackError(invalidSource, "native decoder failure"),
        )
        assertEquals(
            PlaybackErrorCode.PlaybackFailed,
            androidPlaybackError(null, "native decoder failure")?.code,
        )
    }

    @Test
    fun disconnectedTerminalSnapshotBecomesAReducerEvent() {
        assertEquals(
            PlaybackMessage.EnginePlaybackEnded(7L),
            androidTerminalPlaybackMessage(Player.STATE_ENDED, 7L, null),
        )
        assertEquals(
            PlaybackErrorCode.PlaybackFailed,
            (androidTerminalPlaybackMessage(Player.STATE_IDLE, 8L, "decoder") as
                PlaybackMessage.EngineFailed).error.code,
        )
        assertEquals(null, androidTerminalPlaybackMessage(Player.STATE_READY, 9L, null))
    }

}
