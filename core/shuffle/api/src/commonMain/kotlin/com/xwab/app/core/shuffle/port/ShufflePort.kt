package com.xwab.app.core.shuffle.port

import kotlinx.coroutines.flow.Flow

/**
 * Read access to the cognitive-shuffle decks. The words, and the spoken track made from them, stay
 * private to this module; screens play a deck through the session.
 */
interface ShufflePort {
    fun observeDecks(): Flow<List<ShuffleDeck>>
    fun observeDeck(deckId: ShuffleDeckId): Flow<ShuffleDeck?>
}
