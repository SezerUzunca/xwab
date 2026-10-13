package com.xwab.app.feature.shuffle

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId

/** Loading and content states owned by this feature. */
internal sealed interface ShuffleUiState {
    data object Loading : ShuffleUiState

    data class Ready(val value: ShuffleState) : ShuffleUiState
}

/** Content available in [ShuffleUiState.Ready]. */
internal data class ShuffleState(
    val decks: List<ShuffleDeck> = emptyList(),
    /** The deck the session was last asked for, or null when it is on a sound, a story or nothing. */
    val requestedDeckId: ShuffleDeckId? = null,
    /** What a deck's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested deck is wanted but not audible yet; the first time, its words are being spoken. */
    val isPreparing: Boolean = false,
    /**
     * The session's failure, carried as the session reports it. A failed lookup releases the
     * session's claim, so the deck it belongs to is found through [deckFailure] rather than
     * [requestedDeckId].
     */
    val playbackFailure: PlaybackFailure? = null,
) {
    /** Whether any deck is wanted at all: what a timer set on this screen asks before starting one. */
    val isAnyDeckPlaying: Boolean get() = requestedDeckId != null && playIntent

    fun isDeckPlaying(deckId: ShuffleDeckId): Boolean = requestedDeckId == deckId && playIntent

    fun isDeckPreparing(deckId: ShuffleDeckId): Boolean = requestedDeckId == deckId && isPreparing

    fun deckFailure(deckId: ShuffleDeckId): PlaybackFailure? =
        playbackFailure?.takeIf { it.itemId == playbackItemOf(deckId) }
}

internal fun playbackItemOf(deckId: ShuffleDeckId) = PlaybackItemId(SHUFFLE_PLAYBACK_KIND, deckId.value)
