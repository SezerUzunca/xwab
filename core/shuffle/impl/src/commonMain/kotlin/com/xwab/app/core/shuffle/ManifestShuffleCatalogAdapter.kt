package com.xwab.app.core.shuffle

import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.core.shuffle.port.ShufflePort
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves deck metadata from one immutable manifest. */
@SingleIn(ShuffleScope::class)
@Inject
internal class ManifestShuffleCatalogAdapter internal constructor(decks: List<ShuffleDeck>) : ShufflePort {
    init {
        val duplicates = decks.groupBy(ShuffleDeck::id).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Shuffle deck ids must be unique: ${duplicates.joinToString { it.value }}"
        }
    }

    private val allDecks = flowOf(decks)

    override fun observeDecks(): Flow<List<ShuffleDeck>> = allDecks
    override fun observeDeck(deckId: ShuffleDeckId): Flow<ShuffleDeck?> =
        allDecks.map { values -> values.find { it.id == deckId } }
}
