package com.xwab.app.feature.nowplaying.domain

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class NowPlayingContent(
    val playback: PlaybackSummary,
    val sleepTimerRemainingMs: Long?,
)

/** Joins session updates and timer ticks without depending on a content catalog. */
internal class ObserveNowPlayingContentUseCase(private val playbackPort: PlaybackPort) {
    operator fun invoke(): Flow<NowPlayingContent> = combine(
        playbackPort.playback,
        playbackPort.sleepTimerRemainingMs,
    ) { playback, remainingMs -> NowPlayingContent(playback, remainingMs) }
        .distinctUntilChanged()
}
