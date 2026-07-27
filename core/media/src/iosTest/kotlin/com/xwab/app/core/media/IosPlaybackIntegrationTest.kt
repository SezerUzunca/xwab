@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.media

import platform.MediaPlayer.MPRemoteCommandCenter
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosPlaybackIntegrationTest {

    @Test
    fun remoteCommandsAreDisabledAtCreationAndRelease() {
        val session = AppleMediaSession({}, {}, {}, {})
        val commands = MPRemoteCommandCenter.sharedCommandCenter()

        assertFalse(commands.playCommand.enabled)
        assertFalse(commands.pauseCommand.enabled)
        assertFalse(commands.togglePlayPauseCommand.enabled)

        session.setCommandsEnabled(true)
        assertTrue(commands.playCommand.enabled)
        assertTrue(commands.pauseCommand.enabled)
        assertTrue(commands.togglePlayPauseCommand.enabled)

        session.release()
        assertFalse(commands.playCommand.enabled)
        assertFalse(commands.pauseCommand.enabled)
        assertFalse(commands.togglePlayPauseCommand.enabled)
    }

    @Test
    fun stoppingAnEmptyEngineCompletesWithoutRebuildingAQueue() {
        var completion: Boolean? = null
        val engine = IosPlaybackEngine({}, {}, { _, _ -> }, {})

        engine.stop { completion = it }

        assertFalse(completion ?: true)
        assertFalse(engine.hasCurrentItem)
        engine.release()
    }
}
