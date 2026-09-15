@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.playback.platform

import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.projection.NowPlayingPublicationKey
import com.xwab.app.core.playback.projection.NowPlayingUpdateAction
import com.xwab.app.core.playback.projection.decideNowPlayingUpdate
import platform.MediaPlayer.*

/** Publishes Lock Screen and Control Center metadata for the active source. */
internal class NowPlayingInfoPublisher {
    private val infoCenter = MPNowPlayingInfoCenter.defaultCenter()

    private var released = false
    private var lastPublishedKey: NowPlayingPublicationKey? = null

    fun publish(
        source: AudioSource?,
        phase: PlaybackPhase,
        durationMs: Long?,
        positionMs: Long,
        isPlaying: Boolean,
        force: Boolean = false,
    ) {
        if (released) return

        when (decideNowPlayingUpdate(lastPublishedKey, source, phase, isPlaying, force)) {
            NowPlayingUpdateAction.None -> return
            NowPlayingUpdateAction.Clear -> {
                clearNowPlaying()
                return
            }
            NowPlayingUpdateAction.Publish -> Unit
        }
        source ?: return

        lastPublishedKey = NowPlayingPublicationKey(source.id, phase, isPlaying)
        publishNowPlaying(source, durationMs, positionMs, isPlaying)
    }

    fun release() {
        if (released) return
        released = true
        clearNowPlaying()
    }

    private fun clearNowPlaying() {
        lastPublishedKey = null
        infoCenter.nowPlayingInfo = null
    }

    private fun publishNowPlaying(
        source: AudioSource,
        durationMs: Long?,
        positionMs: Long,
        isPlaying: Boolean,
    ) {
        val nowPlayingInfo = mutableMapOf<Any?, Any?>()

        source.title?.let { nowPlayingInfo[MPMediaItemPropertyTitle] = it }
        source.artist?.let { nowPlayingInfo[MPMediaItemPropertyArtist] = it }
        durationMs?.let {
            nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = it / 1_000.0
        }
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = positionMs / 1_000.0
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0

        infoCenter.nowPlayingInfo = nowPlayingInfo
    }
}
