package com.xwab.app.core.playbackengine.platform

import androidx.media3.common.Player
import com.xwab.app.core.playbackengine.api.PlaybackPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the Android-specific helper functions that remain after the
 * migration to the unified [PlaybackReducer].
 *
 * State-transition tests previously in this file have been moved to
 * [PlaybackReducerTest] in `commonTest`.
 */
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

}
