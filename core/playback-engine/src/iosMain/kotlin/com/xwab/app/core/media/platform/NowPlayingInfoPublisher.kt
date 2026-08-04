@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.media.platform

import com.xwab.app.core.media.api.AudioSource
import com.xwab.app.core.media.api.PlaybackPhase
import com.xwab.app.core.media.projection.NowPlayingPublicationKey
import com.xwab.app.core.media.projection.NowPlayingUpdateAction
import com.xwab.app.core.media.projection.decideNowPlayingUpdate
import platform.MediaPlayer.*

/** Publishes Lock Screen and Control Center metadata for the active source. */
internal class NowPlayingInfoPublisher {
    private val infoCenter = MPNowPlayingInfoCenter.defaultCenter()

    private var released = false
    private var nowPlayingSource: AudioSource? = null
    private var nowPlayingDurationMs: Long? = null
    private var nowPlayingPositionMs: Long = 0L
    private var nowPlayingIsPlaying = false
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
        nowPlayingSource = source
        nowPlayingDurationMs = durationMs
        nowPlayingPositionMs = positionMs
        nowPlayingIsPlaying = isPlaying
        publishNowPlaying()
    }

    fun release() {
        if (released) return
        released = true
        clearNowPlaying()
    }

    private fun clearNowPlaying() {
        nowPlayingSource = null
        lastPublishedKey = null
        infoCenter.nowPlayingInfo = null
    }

    private fun publishNowPlaying() {
        val source = nowPlayingSource ?: return
        val nowPlayingInfo = mutableMapOf<Any?, Any?>()

        source.title?.let { nowPlayingInfo[MPMediaItemPropertyTitle] = it }
        source.artist?.let { nowPlayingInfo[MPMediaItemPropertyArtist] = it }
        nowPlayingDurationMs?.let {
            nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = it / 1_000.0
        }
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = nowPlayingPositionMs / 1_000.0
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = if (nowPlayingIsPlaying) 1.0 else 0.0

        infoCenter.nowPlayingInfo = nowPlayingInfo
    }
}
