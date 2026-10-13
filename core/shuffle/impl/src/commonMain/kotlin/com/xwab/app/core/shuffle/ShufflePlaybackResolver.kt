// This module answers the session's resolver contract, so it opts in to it. It never calls the
// session itself: `checkArchitecture` keeps a contributor from also being a consumer.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.core.shuffle.port.ShufflePort
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/**
 * Turns a deck into a track the platform player can open: words this module picks, spoken by the
 * device's voice through speech.
 *
 * Every resolve draws a fresh selection of the deck's words, without repeating one, in a new order,
 * so no two nights are the same sequence; the session resolves again only when it is not already
 * holding the track, so pausing and resuming keeps the one that was playing. Each word starts the
 * deck's interval after the one before: half a second of quiet, the word, then silence — the
 * listener's time to picture it.
 *
 * Speech takes a few seconds the first time a voice speaks a deck's words; the session reports the
 * item as preparing meanwhile. An unknown deck is `NotFound`; one speech could not voice — no voice
 * for its language, no room for the track — is `Unavailable`, which the screen offers to retry.
 */
@SingleIn(ShuffleScope::class)
@Inject
internal class ShufflePlaybackResolver internal constructor(
    private val catalog: ShufflePort,
    wordLists: List<ShuffleWordList>,
    private val speech: SpeechPort,
    private val random: Random,
) : PlaybackItemResolver {
    init {
        val duplicates = wordLists.groupBy(ShuffleWordList::deckId).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Shuffle word lists must be unique: ${duplicates.joinToString()}" }
    }

    private val wordListsByDeck = wordLists.associateBy(ShuffleWordList::deckId)

    override suspend fun resolve(value: String): ItemResolution {
        val deck = catalog.observeDeck(ShuffleDeckId(value)).first()
        val wordList = wordListsByDeck[value]
        return when {
            deck == null -> ItemResolution.NotFound
            // A defensive answer for a manifest gap, which the manifest test is there to prevent.
            wordList == null -> ItemResolution.Unavailable("the deck has no words")
            else -> when (val result = speech.render(requestFor(deck, wordList))) {
                is SpeechResult.Rendered -> ItemResolution.Resolved(
                    uri = result.path,
                    title = deck.title,
                    // A deck is listed and announced under the same name; it has no second one.
                    displayName = deck.title,
                    artist = null,
                    policy = SHUFFLE_POLICY,
                )
                is SpeechResult.Unavailable -> ItemResolution.Unavailable(result.reason)
            }
        }
    }

    private fun requestFor(deck: ShuffleDeck, wordList: ShuffleWordList) = SpeechRequest(
        folder = SHUFFLE_PLAYBACK_KIND,
        name = deck.id.value,
        languageTag = deck.languageTag,
        segments = wordList.words.shuffled(random).take(wordList.wordsPerTrack).flatMap { word ->
            listOf(
                SpeechSegment.Pause(LEAD_IN_SECONDS),
                SpeechSegment.Spoken(word, minSeconds = deck.wordIntervalSeconds - LEAD_IN_SECONDS),
            )
        },
        // A fresh selection every time is the point; the same track twice would not be a shuffle.
        reuse = false,
    )
}

/** A track loops until the sleep timer stops it, as a sleep sound does; it has no end of its own. */
private val SHUFFLE_POLICY = PlaybackPolicy(looping = true)

/** Half a second of quiet before each word, so the track does not open on a syllable. */
private const val LEAD_IN_SECONDS = 0.5
