package com.xwab.app.feature.nap.domain

import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class NapContent(val naps: List<Nap>, val playback: PlaybackSummary)

/** The naps beside the session, so each nap's control shows what the session is doing with it. */
@Inject
internal class ObserveNapContentUseCase(
    private val napPort: NapPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(): Flow<NapContent> = combine(
        napPort.observeNaps(),
        playbackPort.playback,
    ) { naps, playback -> NapContent(naps, playback) }
}
