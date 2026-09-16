@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.playback.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionMediaServicesWereResetNotification
import platform.Foundation.NSNotificationCenter
import platform.MediaPlayer.MPRemoteCommandCenter

class IosPlaybackIntegrationTest {

    @Test
    fun remoteCommandsAreDisabledAtCreationAndRelease() {
        val session = AppleMediaSession({}, {}, {}, {}, {})
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

    @Test
    fun mediaServicesResetIsForwardedUntilTheSessionIsReleased() {
        var resetCount = 0
        val session = AppleMediaSession({}, {}, {}, {}, { resetCount += 1 })
        val notifications = NSNotificationCenter.defaultCenter

        notifications.postNotificationName(
            AVAudioSessionMediaServicesWereResetNotification,
            AVAudioSession.sharedInstance(),
        )
        assertEquals(1, resetCount)

        session.release()
        notifications.postNotificationName(
            AVAudioSessionMediaServicesWereResetNotification,
            AVAudioSession.sharedInstance(),
        )
        assertEquals(1, resetCount)
    }
}
