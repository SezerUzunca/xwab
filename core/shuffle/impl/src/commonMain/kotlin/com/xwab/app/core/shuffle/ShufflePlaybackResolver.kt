// This module answers the session's resolver contract, so it opts in to it. It never calls the
// session itself: `checkArchitecture` keeps a contributor from also being a consumer.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import co.touchlab.kermit.Logger
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.core.shuffle.port.ShufflePort
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

/**
 * Turns a deck into a track the platform player can open: a file this module has just spoken.
 *
 * Unlike a sound or a story there is no address to look up. The track is made here, on the device,
 * which takes a few seconds the first time a voice speaks a deck's words; the session reports the
 * item as preparing meanwhile, as it does for a stream that is still connecting.
 *
 * An unknown deck is `NotFound`. A deck that could not be spoken — the device has no voice for its
 * language, or no room to write the track — is `Unavailable`, which the screen offers to retry.
 */
@SingleIn(ShuffleScope::class)
@Inject
internal class ShufflePlaybackResolver internal constructor(
    private val catalog: ShufflePort,
    wordLists: List<ShuffleWordList>,
    private val renderer: ShuffleTrackRenderer,
    logger: Logger,
) : PlaybackItemResolver {
    init {
        val duplicates = wordLists.groupBy(ShuffleWordList::deckId).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Shuffle word lists must be unique: ${duplicates.joinToString()}" }
    }

    private val logger = logger.withTag("ShufflePlaybackResolver")
    private val wordListsByDeck = wordLists.associateBy(ShuffleWordList::deckId)

    override suspend fun resolve(value: String): ItemResolution {
        val deck = catalog.observeDeck(ShuffleDeckId(value)).first()
        val wordList = wordListsByDeck[value]
        return when {
            deck == null -> ItemResolution.NotFound
            // A defensive answer for a manifest gap, which the manifest test is there to prevent.
            wordList == null -> ItemResolution.Unavailable("the deck has no words")
            else -> render(deck, wordList)
        }
    }

    private suspend fun render(deck: ShuffleDeck, wordList: ShuffleWordList): ItemResolution = try {
        val track = renderer.render(deck, wordList)
        ItemResolution.Resolved(
            uri = track.toString(),
            title = deck.title,
            // A deck is listed and announced under the same name; it has no second one.
            displayName = deck.title,
            artist = null,
            policy = SHUFFLE_POLICY,
        )
    } catch (failure: ShuffleRenderException) {
        logger.w(failure) { "Could not render the ${deck.id} track." }
        ItemResolution.Unavailable(failure.message)
    }
}

/** A track loops until the sleep timer stops it, as a sleep sound does; it has no end of its own. */
private val SHUFFLE_POLICY = PlaybackPolicy(looping = true)
