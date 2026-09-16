@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package com.xwab.app.core.playback.platform

import com.xwab.app.core.playback.store.LatestOperationGate
import com.xwab.app.core.playback.store.PLAYBACK_READINESS_TIMEOUT_MS
import kotlin.native.ref.WeakReference
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.cinterop.readValue
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Owns AVQueuePlayer, AVPlayerLooper, and their native observation lifecycle. */
internal class IosPlaybackEngine(
    private val onStateChanged: () -> Unit,
    private val onPlaybackEnded: (Long) -> Unit,
    private val onPlaybackFailed: (Long, String?) -> Unit,
    private val onReadinessTimedOut: (Long) -> Unit,
) {
    private var player = AVQueuePlayer()
    private val notificationCenter = NSNotificationCenter.defaultCenter

    private var looper: AVPlayerLooper? = null
    private var activeAsset: AVAsset? = null
    private var activeUrl: NSURL? = null
    private var readinessOperationId: Long? = null
    private var readinessStartedAt: TimeMark? = null
    private var loopPreparationPending = false
    private var loopPreparationErrorMessage: String? = null
    private var loopPreparationPositionMs = 0L
    private var loopPreparationVersion: Long? = null
    private var loopPreparationCompletion: ((Boolean) -> Unit)? = null
    private var stateObservationTimer: NSTimer? = null
    private var stateObservationIntervalSeconds: Double? = null
    private var minimumObservationTicks = 0
    private var lastObservedState: EngineObservation? = null
    private val operationGate = LatestOperationGate()
    private var observationSuspendedForFailure = false
    private var released = false

    var currentOperationId: Long = 0L
        private set

    val hasCurrentItem: Boolean
        get() = player.currentItem != null

    val isReadyToPlay: Boolean
        get() = !loopPreparationPending &&
            player.currentItem?.status == AVPlayerItemStatusReadyToPlay

    val isWaitingToPlay: Boolean
        get() = player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate

    val isPlaying: Boolean
        get() = player.timeControlStatus == AVPlayerTimeControlStatusPlaying

    val hasItemFailure: Boolean
        get() = player.currentItem?.status == AVPlayerItemStatusFailed

    val itemErrorMessage: String?
        get() = player.currentItem?.error?.localizedDescription

    val loopErrorMessage: String?
        get() = loopPreparationErrorMessage ?: looper?.error?.localizedDescription

    var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0.0f, 1.0f)
        }

    private val endObserver = run {
        val weakThis = WeakReference(this)
        notificationCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            weakThis.get()?.let { self ->
                if (self.activeItemFrom(notification) != null) {
                    self.onPlaybackEnded(self.currentOperationId)
                }
            }
        }
    }

    private val failureObserver = run {
        val weakThis = WeakReference(this)
        notificationCenter.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification ->
            weakThis.get()?.let { self ->
                self.activeItemFrom(notification)?.let { failedItem ->
                    self.onPlaybackFailed(
                        self.currentOperationId,
                        failedItem.error?.localizedDescription ?: self.itemErrorMessage,
                    )
                }
            }
        }
    }

    fun load(uri: String, looping: Boolean, operationId: Long): Boolean {
        require(operationId > 0L) { "Playback operation id must be positive." }
        operationGate.begin()
        currentOperationId = operationId
        observationSuspendedForFailure = false
        minimumObservationTicks = 0
        clearReadinessObservation()
        stopNativeStateObservation()
        lastObservedState = null
        loopPreparationErrorMessage = null
        player.pause()
        clearQueue()

        val url = if (uri.startsWith('/')) {
            NSURL.fileURLWithPath(uri)
        } else {
            NSURL.URLWithString(uri) ?: return false
        }
        val item = AVPlayerItem(uRL = url)
        activeUrl = url
        activeAsset = item.asset
        attachItem(item, looping)
        observeReadiness()
        observeNativeState()
        publishObservedStateIfChanged(force = true)
        return true
    }

    fun play() {
        if (!hasCurrentItem) return
        observationSuspendedForFailure = false
        minimumObservationTicks = PLAY_TRANSITION_OBSERVATION_TICKS
        player.play()
        observeReadiness()
        observeNativeState()
        publishObservedStateIfChanged(force = true)
    }

    fun pause(notify: Boolean = true) {
        operationGate.begin()
        minimumObservationTicks = 0
        player.pause()
        if (notify) publishObservedStateIfChanged(force = true)
    }

    fun seekTo(
        positionMs: Long,
        completion: (Boolean) -> Unit,
    ) {
        rebuildQueue(
            looping = looper != null,
            positionMs = positionMs,
            completion = completion,
        )
    }

    fun setLooping(
        enabled: Boolean,
        positionMs: Long,
        completion: (Boolean) -> Unit,
    ) {
        rebuildQueue(enabled, positionMs, completion)
    }

    fun stop(completion: (Boolean) -> Unit) {
        minimumObservationTicks = 0
        player.pause()
        val version = operationGate.begin()
        if (!hasCurrentItem) {
            completion(false)
            publishObservedStateIfChanged(force = true)
            return
        }
        // Keep the existing AVPlayerItem and looper. Rebuilding the queue here
        // briefly changes the engine back to Loading after every timer expiry.
        seekNative(0L, version) { finished ->
            completion(finished)
            publishObservedStateIfChanged(force = true)
        }
    }

    fun currentPositionMs(): Long = CMTimeGetSeconds(player.currentTime())
        .takeIf { it.isFinite() && it >= 0.0 }
        ?.times(1_000.0)
        ?.toLong()
        ?: 0L

    fun durationMs(): Long? = player.currentItem
        ?.duration
        ?.let(::CMTimeGetSeconds)
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.times(1_000.0)
        ?.toLong()

    fun release() {
        if (released) return
        released = true
        minimumObservationTicks = 0
        clearReadinessObservation()
        stopNativeStateObservation()
        operationGate.begin()
        notificationCenter.removeObserver(endObserver)
        notificationCenter.removeObserver(failureObserver)
        player.pause()
        clearQueue()
    }

    fun suspendObservationForFailure() {
        observationSuspendedForFailure = true
        minimumObservationTicks = 0
        clearReadinessObservation()
        stopNativeStateObservation()
    }

    /** Recreates native playback objects after AVAudioSession media services reset. */
    fun resetAfterMediaServicesWereReset() {
        operationGate.begin()
        minimumObservationTicks = 0
        clearReadinessObservation()
        stopNativeStateObservation()
        player.pause()
        clearQueue()
        player = AVQueuePlayer()
        lastObservedState = null
        observationSuspendedForFailure = false
        onStateChanged()
    }

    private fun observeReadiness(operationId: Long = currentOperationId) {
        if (observationSuspendedForFailure) return
        if (loopPreparationPending || player.currentItem?.status == AVPlayerItemStatusUnknown) {
            if (readinessOperationId != operationId) {
                readinessOperationId = operationId
                readinessStartedAt = TimeSource.Monotonic.markNow()
            }
        } else {
            clearReadinessObservation()
        }
    }

    private fun clearReadinessObservation() {
        readinessOperationId = null
        readinessStartedAt = null
    }

    private fun updateReadinessObservation() {
        val operationId = readinessOperationId ?: return
        completeLoopPreparationIfReady()
        if (released || isReadyToPlay || hasItemFailure || loopErrorMessage != null) {
            clearReadinessObservation()
            return
        }
        val elapsedMs = readinessStartedAt?.elapsedNow()?.inWholeMilliseconds ?: return
        if (elapsedMs >= PLAYBACK_READINESS_TIMEOUT_MS) {
            clearReadinessObservation()
            onReadinessTimedOut(operationId)
        }
    }

    private fun clearQueue() {
        looper?.disableLooping()
        looper = null
        loopPreparationPending = false
        loopPreparationErrorMessage = null
        loopPreparationPositionMs = 0L
        loopPreparationVersion = null
        loopPreparationCompletion = null
        activeAsset = null
        activeUrl = null
        player.removeAllItems()
    }

    private fun rebuildQueue(
        looping: Boolean,
        positionMs: Long,
        completion: (Boolean) -> Unit,
    ) {
        val version = operationGate.begin()
        val url = activeUrl ?: run {
            completion(false)
            publishObservedStateIfChanged(force = true)
            return
        }

        player.pause()
        clearQueue()
        val item = AVPlayerItem(uRL = url)
        activeUrl = url
        activeAsset = item.asset
        attachItem(
            item = item,
            looping = looping,
            positionMs = positionMs,
            version = version,
            completion = completion,
        )
        observeReadiness()
        observeNativeState()
        if (!looping) {
            seekNative(positionMs, version) { finished ->
                completion(finished)
                publishObservedStateIfChanged(force = true)
            }
        }
    }

    /**
     * A looping item is first allowed to become ready as a normal queue item. This guarantees its
     * asset duration is known before AVPlayerLooper is created, avoiding the documented blocking
     * initializer and allowing zero-duration media to fail through the normal engine error path.
     */
    private fun attachItem(
        item: AVPlayerItem,
        looping: Boolean,
        positionMs: Long = 0L,
        version: Long? = null,
        completion: ((Boolean) -> Unit)? = null,
    ) {
        if (looping) {
            loopPreparationPending = true
            loopPreparationPositionMs = positionMs
            loopPreparationVersion = version
            loopPreparationCompletion = completion
        }
        player.replaceCurrentItemWithPlayerItem(item)
    }

    private fun completeLoopPreparationIfReady() {
        if (!loopPreparationPending) return
        val item = player.currentItem ?: return
        if (item.status != AVPlayerItemStatusReadyToPlay) return

        val durationSeconds = CMTimeGetSeconds(item.duration)
        if (!durationSeconds.isFinite() || durationSeconds <= 0.0) {
            loopPreparationPending = false
            loopPreparationErrorMessage = "Looping audio must have a finite, positive duration."
            loopPreparationCompletion?.invoke(false)
            loopPreparationCompletion = null
            return
        }

        val positionMs = loopPreparationPositionMs
        val version = loopPreparationVersion
        val completion = loopPreparationCompletion
        player.pause()
        player.removeAllItems()
        looper = AVPlayerLooper(
            player = player,
            templateItem = item,
            timeRange = kCMTimeRangeInvalid.readValue(),
        )
        loopPreparationPending = false
        loopPreparationPositionMs = 0L
        loopPreparationVersion = null
        loopPreparationCompletion = null
        observeReadiness()

        if (completion != null && version != null) {
            seekNative(positionMs, version) { finished ->
                completion(finished)
                publishObservedStateIfChanged(force = true)
            }
        }
    }

    private fun seekNative(
        positionMs: Long,
        version: Long,
        completion: (Boolean) -> Unit,
    ) {
        val seconds = positionMs.coerceAtLeast(0L) / 1_000.0
        player.seekToTime(
            time = CMTimeMakeWithSeconds(seconds, preferredTimescale = 600),
            completionHandler = { finished ->
                // AVFoundation does not guarantee the queue this runs on, but everything
                // it feeds — the store, the published state — is main-thread-only.
                onMainThread {
                    if (!released && operationGate.isCurrent(version)) completion(finished)
                }
            },
        )
    }

    /** Runs [action] on the main thread, without deferring when already there. */
    private fun onMainThread(action: () -> Unit) {
        if (NSThread.isMainThread) {
            action()
        } else {
            dispatch_async(dispatch_get_main_queue()) { action() }
        }
    }

    private fun observeNativeState() {
        updateNativeStateObservation()
    }

    private fun updateNativeStateObservation() {
        val requiredInterval = requiredNativeStateObservationInterval()
        if (released || observationSuspendedForFailure || requiredInterval == null) {
            stopNativeStateObservation()
            return
        }
        if (
            stateObservationTimer != null &&
            stateObservationIntervalSeconds == requiredInterval
        ) {
            return
        }

        stopNativeStateObservation()
        stateObservationIntervalSeconds = requiredInterval
        stateObservationTimer = NSTimer.scheduledTimerWithTimeInterval(
            interval = requiredInterval,
            repeats = true,
            block = {
                if (released) {
                    stopNativeStateObservation()
                } else {
                    updateReadinessObservation()
                    if (minimumObservationTicks > 0) {
                        minimumObservationTicks -= 1
                    }
                    publishObservedStateIfChanged()
                }
            },
        )
    }

    private fun stopNativeStateObservation() {
        stateObservationTimer?.invalidate()
        stateObservationTimer = null
        stateObservationIntervalSeconds = null
    }

    private fun requiredNativeStateObservationInterval(): Double? =
        if (minimumObservationTicks > 0) {
            FAST_STATE_OBSERVATION_INTERVAL_SECONDS
        } else {
            dynamicNativeStateObservationInterval()
        }

    private fun dynamicNativeStateObservationInterval(): Double? = when {
        !hasCurrentItem || hasItemFailure || loopErrorMessage != null -> null
        !isReadyToPlay || isWaitingToPlay -> FAST_STATE_OBSERVATION_INTERVAL_SECONDS
        isPlaying -> PLAYING_STATE_OBSERVATION_INTERVAL_SECONDS
        // A ready, paused item can still transition to failed (for example,
        // after an interrupted resource read). Poll it infrequently so that
        // this failure is surfaced without retaining the former 1 Hz cost.
        else -> PAUSED_READY_STATE_OBSERVATION_INTERVAL_SECONDS
    }

    private fun publishObservedStateIfChanged(force: Boolean = false) {
        if (released) return
        completeLoopPreparationIfReady()
        val observedState = EngineObservation(
            hasCurrentItem = hasCurrentItem,
            isReadyToPlay = isReadyToPlay,
            isWaitingToPlay = isWaitingToPlay,
            isPlaying = isPlaying,
            hasItemFailure = hasItemFailure,
            itemErrorMessage = itemErrorMessage,
            loopErrorMessage = loopErrorMessage,
        )
        if (force || observedState != lastObservedState) {
            lastObservedState = observedState
            onStateChanged()
        }
        if (
            !hasCurrentItem ||
            hasItemFailure ||
            loopErrorMessage != null ||
            isWaitingToPlay ||
            isPlaying
        ) {
            minimumObservationTicks = 0
        }
        updateNativeStateObservation()
    }

    private fun activeItemFrom(notification: NSNotification?): AVPlayerItem? {
        val item = notification?.`object` as? AVPlayerItem ?: return null
        val asset = activeAsset ?: return null
        return item.takeIf { it.asset === asset }
    }

    private data class EngineObservation(
        val hasCurrentItem: Boolean,
        val isReadyToPlay: Boolean,
        val isWaitingToPlay: Boolean,
        val isPlaying: Boolean,
        val hasItemFailure: Boolean,
        val itemErrorMessage: String?,
        val loopErrorMessage: String?,
    )

    private companion object {
        const val FAST_STATE_OBSERVATION_INTERVAL_SECONDS = 0.2
        const val PLAYING_STATE_OBSERVATION_INTERVAL_SECONDS = 1.0
        const val PAUSED_READY_STATE_OBSERVATION_INTERVAL_SECONDS = 5.0
        const val PLAY_TRANSITION_OBSERVATION_TICKS = 10
    }
}
