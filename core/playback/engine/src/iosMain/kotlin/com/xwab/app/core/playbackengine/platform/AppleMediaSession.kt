@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package com.xwab.app.core.playbackengine.platform

import kotlin.native.ref.WeakReference
import platform.AVFAudio.*
import platform.Foundation.*
import platform.MediaPlayer.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Manages the iOS audio session and remote control commands such as AirPods controls.
 */
internal class AppleMediaSession(
    private val onPlayRequested: () -> Unit,
    private val onPauseRequested: () -> Unit,
    private val onToggleRequested: () -> Unit,
    private val onInterruptionBegan: () -> Unit,
) {
    private val audioSession = AVAudioSession.sharedInstance()
    private val notificationCenter = NSNotificationCenter.defaultCenter
    private val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

    private var resumeAfterInterruption = false
    private var playRequested = false
    private var commandsEnabled = false

    private var playTarget: Any? = null
    private var pauseTarget: Any? = null
    private var toggleTarget: Any? = null

    private val interruptionObserver = run {
        val weakThis = WeakReference(this)
        notificationCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = audioSession,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            notification?.let { weakThis.get()?.handleInterruption(it) }
        }
    }

    private val routeChangeObserver = run {
        val weakThis = WeakReference(this)
        notificationCenter.addObserverForName(
            name = AVAudioSessionRouteChangeNotification,
            `object` = audioSession,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            notification?.let { weakThis.get()?.handleRouteChange(it) }
        }
    }

    init {
        setupRemoteCommands()
        setCommandsEnabled(false)
    }

    fun activate(): Boolean {
        val configured = audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
        val activated = configured && audioSession.setActive(true, error = null)
        playRequested = activated
        return activated
    }

    fun setCommandsEnabled(enabled: Boolean) {
        commandsEnabled = enabled
        commandCenter.playCommand.enabled = enabled
        commandCenter.pauseCommand.enabled = enabled
        commandCenter.togglePlayPauseCommand.enabled = enabled
    }

    fun clearInterruptionState() {
        resumeAfterInterruption = false
    }

    fun deactivate() {
        playRequested = false
        audioSession.setActive(
            active = false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = null,
        )
    }

    fun release() {
        setCommandsEnabled(false)
        playRequested = false
        resumeAfterInterruption = false
        notificationCenter.removeObserver(interruptionObserver)
        notificationCenter.removeObserver(routeChangeObserver)
        teardownRemoteCommands()
        deactivate()
    }

    private fun setupRemoteCommands() {
        playTarget = commandCenter.playCommand.addTargetWithHandler { _ ->
            dispatchWhenEnabled(onPlayRequested)
            MPRemoteCommandHandlerStatusSuccess
        }
        pauseTarget = commandCenter.pauseCommand.addTargetWithHandler { _ ->
            dispatchWhenEnabled(onPauseRequested)
            MPRemoteCommandHandlerStatusSuccess
        }
        toggleTarget = commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
            dispatchWhenEnabled(onToggleRequested)
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun dispatchWhenEnabled(action: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            if (commandsEnabled) action()
        }
    }

    private fun teardownRemoteCommands() {
        playTarget?.let { commandCenter.playCommand.removeTarget(it) }
        pauseTarget?.let { commandCenter.pauseCommand.removeTarget(it) }
        toggleTarget?.let { commandCenter.togglePlayPauseCommand.removeTarget(it) }
    }

    private fun handleInterruption(notification: NSNotification) {
        val type = notification.unsignedUserInfoValue(AVAudioSessionInterruptionTypeKey)
        when (type) {
            AVAudioSessionInterruptionTypeBegan -> {
                resumeAfterInterruption = playRequested
                onInterruptionBegan()
            }
            AVAudioSessionInterruptionTypeEnded -> {
                val options = notification.unsignedUserInfoValue(AVAudioSessionInterruptionOptionKey) ?: 0uL
                val shouldResume =
                    resumeAfterInterruption &&
                        options and AVAudioSessionInterruptionOptionShouldResume != 0uL
                resumeAfterInterruption = false
                if (shouldResume) onPlayRequested()
            }
        }
    }

    private fun handleRouteChange(notification: NSNotification) {
        val reason = notification.unsignedUserInfoValue(AVAudioSessionRouteChangeReasonKey)
        if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
            resumeAfterInterruption = false
            onPauseRequested()
        }
    }

    private fun NSNotification.unsignedUserInfoValue(key: String?): ULong? {
        if (key == null) return null
        return (userInfo?.get(key) as? NSNumber)?.unsignedIntegerValue
    }
}
