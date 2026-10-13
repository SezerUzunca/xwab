@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.SpeechSegment
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ShufflePlaybackResolverTest {
    @Test
    fun aDeckBecomesALoopingTrackUnderItsOwnName() = runTest {
        val speech = FakeSpeech()

        val resolved = assertIs<ItemResolution.Resolved>(resolver(speech).resolve(TEST_DECK))

        assertEquals("/cache/speech/tracks/shuffle/test-deck.1.wav", resolved.uri)
        assertEquals("Test Deck", resolved.title)
        assertEquals("Test Deck", resolved.displayName)
        assertNull(resolved.artist)
        assertTrue(resolved.policy.looping, "a track repeats until the sleep timer stops it")
    }

    /** Half a second of quiet, then the word held to the rest of the interval: each word a slot. */
    @Test
    fun eachWordTakesOneIntervalWithoutRepeating() = runTest {
        val speech = FakeSpeech()

        resolver(speech).resolve(TEST_DECK)

        val request = speech.requests.single()
        assertEquals("shuffle", request.folder)
        assertEquals("en-US", request.languageTag)
        assertFalse(request.reuse, "every play is a new selection")
        val words = request.segments.filterIsInstance<SpeechSegment.Spoken>()
        assertEquals(3, words.size)
        assertEquals(words.map { it.text }.toSet().size, words.size, "no word twice in one track")
        assertTrue(words.all { it.text in wordList().words && it.minSeconds == 2.5 })
        assertEquals(List(3) { SpeechSegment.Pause(0.5) }, request.segments.filterIsInstance<SpeechSegment.Pause>())
    }

    @Test
    fun everyPlayDrawsANewOrder() = runTest {
        val speech = FakeSpeech()
        val resolver = resolver(speech, words = wordList(wordsPerTrack = 4))

        repeat(5) { resolver.resolve(TEST_DECK) }

        val orders = speech.requests.map { request ->
            request.segments.filterIsInstance<SpeechSegment.Spoken>().map { it.text }
        }
        assertNotEquals(1, orders.toSet().size, "five plays, one order: $orders")
    }

    @Test
    fun anUnknownDeckIsNotFound() = runTest {
        assertEquals(ItemResolution.NotFound, resolver(FakeSpeech()).resolve("no-such-deck"))
    }

    /** No voice for the deck's language is worth another tap once one is installed. */
    @Test
    fun aDeckSpeechCannotVoiceIsUnavailable() = runTest {
        val resolution = resolver(FakeSpeech(unavailable = "No en-US voice.")).resolve(TEST_DECK)

        assertEquals(ItemResolution.Unavailable("No en-US voice."), resolution)
    }

    @Test
    fun aDeckWithoutWordsIsUnavailable() = runTest {
        assertIs<ItemResolution.Unavailable>(resolver(FakeSpeech(), wordLists = emptyList()).resolve(TEST_DECK))
    }

    @Test
    fun aDeckCanHaveOnlyOneWordList() {
        assertFailsWith<IllegalArgumentException> { resolver(FakeSpeech(), wordLists = listOf(wordList(), wordList())) }
    }

    private fun resolver(
        speech: FakeSpeech,
        words: ShuffleWordList = wordList(),
        wordLists: List<ShuffleWordList> = listOf(words),
    ) = ShufflePlaybackResolver(
        catalog = ManifestShuffleCatalogAdapter(listOf(deck())),
        wordLists = wordLists,
        speech = speech,
        random = Random(1),
    )
}
