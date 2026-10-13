package com.xwab.app.core.shuffle

import com.xwab.app.core.shuffle.port.ShuffleDeckId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShuffleManifestTest {
    @Test
    fun everyDeckHasItsWordsAndEveryWordListItsDeck() {
        assertTrue(shuffleDecks.isNotEmpty())
        assertEquals(shuffleDecks.map { it.id.value }.sorted(), shuffleWordLists.map { it.deckId }.sorted())
    }

    /** The deck ids are stored on devices, in the engine's source id; a rename strands them. */
    @Test
    fun theShippedDeckKeepsItsId() {
        assertEquals(listOf(ShuffleDeckId("everyday-things")), shuffleDecks.map { it.id })
    }

    /**
     * A track is uncompressed: its size is the words it holds times their interval. Even at 48 kHz,
     * the highest rate a voice writes, one track stays under 50 MB of the cache.
     */
    @Test
    fun aTrackStaysASizeTheCacheCanHold() {
        shuffleWordLists.forEach { list ->
            val deck = shuffleDecks.single { it.id.value == list.deckId }
            val bytes = list.wordsPerTrack.toLong() * deck.wordIntervalSeconds * 48_000 * 2
            assertTrue(bytes < 50_000_000, "${list.deckId} renders $bytes bytes")
        }
    }

    @Test
    fun aWordListRefusesWordsThatAreNotPlainLowerCase() {
        assertFailsWith<IllegalArgumentException> { wordList(words = listOf("kite", "Cup", "owl")) }
        assertFailsWith<IllegalArgumentException> { wordList(words = listOf("kite", "cup", "owl.")) }
        assertFailsWith<IllegalArgumentException> { wordList(words = listOf("kite", "cup", "big red owl")) }
        wordList(words = listOf("kite", "cup", "pine cone"))
    }

    @Test
    fun aWordListRefusesRepeatsAndTracksLongerThanItsWords() {
        assertFailsWith<IllegalArgumentException> { wordList(words = listOf("kite", "cup", "kite")) }
        assertFailsWith<IllegalArgumentException> { wordList(words = listOf("kite", "cup"), wordsPerTrack = 3) }
        assertFailsWith<IllegalArgumentException> { wordList(wordsPerTrack = 1) }
        assertFailsWith<IllegalArgumentException> { wordList(deckId = " ") }
    }
}
