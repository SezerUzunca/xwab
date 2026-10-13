package com.xwab.app.core.speech

import com.xwab.app.core.speech.port.NoiseBed
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import okio.BufferedSink

/** A track's segment once its text has been spoken: what actually goes into the file. */
internal sealed interface TrackSegment {
    /** @param minSeconds how long the segment lasts at least; what the speech leaves is silence. */
    class Speech(val pcm: Pcm, val minSeconds: Double) : TrackSegment

    class Pause(val seconds: Double) : TrackSegment
}

/**
 * Writes spoken segments as one mono 16-bit track, with [bed] under all of it if there is one.
 *
 * Every piece of speech is evened out before it is written. Voices speak some words louder than
 * others, and a word that jumps out of the dark is the opposite of what these tracks are for; each
 * one is brought to the same moderate peak and eased in and out, so nothing starts or ends with a
 * click. The track takes the first speech's sample rate; speech at another rate is converted to it.
 *
 * The track is written as it is laid out, so a long one never has to fit in memory twice.
 */
internal fun writeSpeechTrack(
    sink: BufferedSink,
    segments: List<TrackSegment>,
    bed: NoiseBed? = null,
    random: Random = Random.Default,
) {
    val sampleRate = segments.filterIsInstance<TrackSegment.Speech>().firstOrNull()?.pcm?.sampleRate
    requireNotNull(sampleRate) { "A track needs something spoken in it." }
    val parts = segments.map { segment ->
        when (segment) {
            is TrackSegment.Speech -> {
                val spoken = evenedOut(segment.pcm.resampledTo(sampleRate).samples)
                Part(spoken, max(0, secondsToSamples(segment.minSeconds, sampleRate) - spoken.size))
            }
            is TrackSegment.Pause -> Part(EMPTY, secondsToSamples(segment.seconds, sampleRate))
        }
    }
    val total = parts.sumOf { it.speech.size.toLong() + it.silence }

    Wav.writeHeader(sink, sampleRate, total)
    val writer = if (bed == null) PlainWriter(sink) else BedWriter(sink, bed, sampleRate, total, random)
    parts.forEach { part ->
        writer.speech(part.speech)
        writer.silence(part.silence)
    }
}

/** Converts to [rate] by linear interpolation: enough for a voice, which is all this module carries. */
internal fun Pcm.resampledTo(rate: Int): Pcm {
    if (rate == sampleRate || samples.isEmpty()) return Pcm(rate, samples)
    val length = (samples.size.toLong() * rate / sampleRate).toInt()
    val last = samples.lastIndex
    return Pcm(
        rate,
        ShortArray(length) { index ->
            val position = index.toDouble() * sampleRate / rate
            val before = position.toInt()
            val after = min(before + 1, last)
            val fraction = position - before
            (samples[before] + (samples[after] - samples[before]) * fraction).roundToInt().toShort()
        },
    )
}

private class Part(val speech: ShortArray, val silence: Int)

private interface TrackWriter {
    fun speech(samples: ShortArray)
    fun silence(count: Int)
}

/** No bed: speech as it is, and silence a block at a time — most of a track is silence. */
private class PlainWriter(private val sink: BufferedSink) : TrackWriter {
    override fun speech(samples: ShortArray) {
        samples.forEach { sink.writeShortLe(it.toInt()) }
    }

    override fun silence(count: Int) {
        var remaining = count.toLong() * BYTES_PER_SAMPLE
        while (remaining > 0) {
            val block = min(remaining, SILENCE.size.toLong()).toInt()
            sink.write(SILENCE, 0, block)
            remaining -= block
        }
    }
}

/**
 * Speech over brown noise: white noise summed and leaked, which leaves the low, even rumble of a
 * distant fan rather than a hiss. It rises over the bed's fade at the start and falls over it at
 * the end, so the track neither starts nor stops abruptly.
 */
private class BedWriter(
    private val sink: BufferedSink,
    bed: NoiseBed,
    sampleRate: Int,
    private val total: Long,
    private val random: Random,
) : TrackWriter {
    private val peak = bed.level * Short.MAX_VALUE
    private val fade = max(1L, (bed.fadeSeconds * sampleRate).toLong())
    private var position = 0L
    private var brown = 0.0

    override fun speech(samples: ShortArray) {
        samples.forEach { write(it.toInt()) }
    }

    override fun silence(count: Int) {
        repeat(count) { write(0) }
    }

    private fun write(voice: Int) {
        val white = random.nextDouble() * 2 - 1
        brown = (brown + BROWN_STEP * white) / BROWN_LEAK
        val envelope = min(1.0, min(position, total - 1 - position).toDouble() / fade)
        val noise = (brown * BROWN_GAIN).coerceIn(-1.0, 1.0) * peak * envelope
        sink.writeShortLe((voice + noise).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()))
        position++
    }
}

/** Brings [samples] to [TARGET_PEAK], with short fades at both ends. */
private fun evenedOut(samples: ShortArray): ShortArray {
    val peak = samples.maxOfOrNull { abs(it.toInt()) } ?: 0
    if (peak == 0) return samples
    val gain = TARGET_PEAK / peak
    val fade = min(samples.size / 2, FADE_SAMPLES)
    return ShortArray(samples.size) { index ->
        val edge = min(index, samples.lastIndex - index)
        val envelope = if (edge < fade) edge.toDouble() / fade else 1.0
        (samples[index] * gain * envelope).roundToInt().toShort()
    }
}

private fun secondsToSamples(seconds: Double, sampleRate: Int): Int = (seconds * sampleRate).roundToInt()

/** 0.4 of full scale, about 8 dB below it: clearly heard, never the loudest thing in a dark room. */
private const val TARGET_PEAK = 13_107.0

/**
 * The fade's length in samples. Counted in samples rather than time on purpose: a voice's rate is
 * between 16 and 48 kHz, which keeps it between 5 and 15 ms — long enough to hide the edge of a
 * piece of speech, short enough not to soften it.
 */
private const val FADE_SAMPLES = 240

/** How far each white sample moves the brown one, and how much of it leaks away again. */
private const val BROWN_STEP = 0.02
private const val BROWN_LEAK = 1.02

/** Brings the leaky sum, which wanders within about ±0.3, up to the bed's level. */
private const val BROWN_GAIN = 3.5

private const val BYTES_PER_SAMPLE = 2
private const val SILENCE_BLOCK_BYTES = 8_192
private val SILENCE = ByteArray(SILENCE_BLOCK_BYTES)
private val EMPTY = ShortArray(0)
