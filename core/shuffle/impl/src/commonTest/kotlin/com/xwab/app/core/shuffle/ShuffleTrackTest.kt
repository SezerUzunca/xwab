package com.xwab.app.core.shuffle

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okio.Buffer

class ShuffleTrackTest {
    /**
     * Ten thousand samples a second and a one-second interval: each slot is 10,000 samples, the
     * first 5,000 of them the lead-in.
     */
    @Test
    fun eachWordStartsItsOwnSlotAfterTheLeadIn() {
        val track = track(listOf(word(3_000), word(3_000)), intervalSeconds = 1)

        assertEquals(RATE, track.sampleRate)
        assertEquals(2 * SLOT, track.samples.size, "a slot per word, however short the word")
        repeat(2) { slot ->
            val start = slot * SLOT
            assertTrue(track.samples.slice(start until start + LEAD_IN).all { it == 0.toShort() }, "lead-in $slot")
            assertEquals(0, track.samples[start + LEAD_IN].toInt(), "a word fades in from silence")
            assertTrue(abs(track.samples[start + LEAD_IN + 1_500].toInt()) > 0, "word $slot is in its slot")
            assertTrue(
                track.samples.slice(start + LEAD_IN + 3_000 until start + SLOT).all { it == 0.toShort() },
                "the rest of slot $slot is the listener's",
            )
        }
    }

    /** A quiet word and a loud one come out at the same level. */
    @Test
    fun everyWordIsBroughtToTheSamePeak() {
        val track = track(listOf(word(3_000, level = 500), word(3_000, level = 20_000)), intervalSeconds = 1)

        val peaks = List(2) { slot ->
            track.samples.slice(slot * SLOT until (slot + 1) * SLOT).maxOf { abs(it.toInt()) }
        }

        assertEquals(peaks[0], peaks[1])
        assertEquals(13_107, peaks[0])
    }

    /** A word cannot push the rest of the track late; it is cut where its slot ends. */
    @Test
    fun aWordLongerThanItsSlotIsCutAtTheSlotsEnd() {
        val track = track(listOf(word(8_000), word(1_000)), intervalSeconds = 1)

        assertEquals(2 * SLOT, track.samples.size)
        assertEquals(0, track.samples[SLOT - 1].toInt(), "the cut word still fades out")
        assertTrue(abs(track.samples[SLOT + LEAD_IN + 500].toInt()) > 0, "the next word is on time")
    }

    /** The track takes the first word's rate; a word spoken at another is converted, not misplayed. */
    @Test
    fun aWordAtAnotherRateIsConverted() {
        val slow = Pcm(RATE / 2, ShortArray(1_500) { 1_000 })

        val track = track(listOf(word(3_000), slow), intervalSeconds = 1)

        assertEquals(RATE, track.sampleRate)
        val second = track.samples.slice(SLOT until 2 * SLOT)
        assertEquals(3_000, second.count { it != 0.toShort() } + 2, "same length in time, both fade ends at zero")
    }

    private fun track(words: List<Pcm>, intervalSeconds: Int): Pcm {
        val buffer = Buffer()
        writeShuffleTrack(buffer, words, intervalSeconds)
        return Wav.decode(buffer.readByteArray())
    }

    private fun word(length: Int, level: Int = 1_000) = Pcm(RATE, ShortArray(length) { level.toShort() })

    private companion object {
        const val RATE = 10_000
        const val SLOT = RATE
        const val LEAD_IN = RATE / 2
    }
}
