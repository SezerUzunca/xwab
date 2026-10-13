package com.xwab.app.feature.shuffle.domain

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShufflePort
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class ShuffleContent(val decks: List<ShuffleDeck>, val playback: PlaybackSummary)

/** The decks beside the session, so each deck's control shows what the session is doing with it. */
@Inject
internal class ObserveShuffleContentUseCase(
    private val shufflePort: ShufflePort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(): Flow<ShuffleContent> = combine(
        shufflePort.observeDecks(),
        playbackPort.playback,
    ) { decks, playback -> ShuffleContent(decks, playback) }
}
