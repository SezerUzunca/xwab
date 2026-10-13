package com.xwab.app.core.speech

import com.xwab.app.core.speech.port.NoiseBed
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import okio.Buffer

class SpeechTrackTest {
    /**
     * The shuffle's layout: half a second of quiet, then a word that holds its slot to the full
     * interval. Ten thousand samples a second, so each slot is 10,000 samples.
     */
    @Test
    fun speechHoldsItsSegmentAndSilenceFillsTheRest() {
        val track = track(
            TrackSegment.Pause(0.5),
            TrackSegment.Speech(speech(3_000), minSeconds = 0.5),
            TrackSegment.Pause(0.5),
            TrackSegment.Speech(speech(3_000), minSeconds = 0.5),
        )

        assertEquals(RATE, track.sampleRate)
        assertEquals(2 * RATE, track.samples.size, "pause, speech held to its minimum, twice")
        repeat(2) { slot ->
            val start = slot * RATE
            assertTrue(track.samples.slice(start until start + HALF).all { it == 0.toShort() }, "pause $slot")
            assertEquals(0, track.samples[start + HALF].toInt(), "speech fades in from silence")
            assertTrue(abs(track.samples[start + HALF + 1_500].toInt()) > 0, "speech $slot is in place")
            assertTrue(
                track.samples.slice(start + HALF + 3_000 until start + RATE).all { it == 0.toShort() },
                "the rest of slot $slot is silence",
            )
        }
    }

    /** Speech longer than its minimum is not cut; the segment simply lasts as long as it does. */
    @Test
    fun speechLongerThanItsMinimumIsKeptWhole() {
        val track = track(TrackSegment.Speech(speech(8_000), minSeconds = 0.5), TrackSegment.Pause(0.1))

        assertEquals(8_000 + 1_000, track.samples.size)
    }

    /** A quiet sentence and a loud one come out at the same level. */
    @Test
    fun everyPieceOfSpeechIsBroughtToTheSamePeak() {
        val track = track(
            TrackSegment.Speech(speech(3_000, level = 500), 0.0),
            TrackSegment.Speech(speech(3_000, level = 20_000), 0.0),
        )

        val peaks = listOf(0 until 3_000, 3_000 until 6_000).map { range ->
            track.samples.slice(range).maxOf { abs(it.toInt()) }
        }

        assertEquals(listOf(13_107, 13_107), peaks)
    }

    /** The track takes the first speech's rate; speech at another is converted, not misplayed. */
    @Test
    fun speechAtAnotherRateIsConverted() {
        val slow = Pcm(RATE / 2, ShortArray(1_500) { 1_000 })

        val track = track(TrackSegment.Speech(speech(3_000), 0.0), TrackSegment.Speech(slow, 0.0))

        assertEquals(RATE, track.sampleRate)
        assertEquals(6_000, track.samples.size, "same length in time")
    }

    /**
     * The bed is under everything, silences included, rises from nothing at the start and falls to
     * nothing at the end, and stays well under the speech.
     */
    @Test
    fun aBedRunsUnderTheWholeTrackAndFadesAtBothEnds() {
        val bed = NoiseBed(level = 0.05, fadeSeconds = 0.2)

        val track = track(
            TrackSegment.Pause(1.0),
            TrackSegment.Speech(speech(3_000), 0.0),
            TrackSegment.Pause(1.0),
            bed = bed,
        )

        val samples = track.samples
        assertEquals(0, samples.first().toInt(), "the bed starts from nothing")
        assertEquals(0, samples.last().toInt(), "and ends at nothing")
        assertTrue(samples.slice(3_000 until 7_000).any { it != 0.toShort() }, "the bed fills the silence")
        val bedPeak = samples.slice(2_000 until RATE).maxOf { abs(it.toInt()) }
        assertTrue(bedPeak <= 0.05 * Short.MAX_VALUE + 1, "the bed stays at its level: $bedPeak")
        assertTrue(samples.slice(RATE until RATE + 3_000).maxOf { abs(it.toInt()) } > 13_107 - bedPeak)
    }

    @Test
    fun aTrackNeedsSomethingSpoken() {
        assertFailsWith<IllegalArgumentException> { track(TrackSegment.Pause(1.0)) }
    }

    private fun track(vararg segments: TrackSegment, bed: NoiseBed? = null): Pcm {
        val buffer = Buffer()
        writeSpeechTrack(buffer, segments.toList(), bed, Random(SEED))
        return Wav.decode(buffer.readByteArray())
    }

    private fun speech(length: Int, level: Int = 1_000) = Pcm(RATE, ShortArray(length) { level.toShort() })

    private companion object {
        const val RATE = 10_000
        const val HALF = RATE / 2
        const val SEED = 3
    }
}
