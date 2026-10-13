package com.xwab.app.core.shuffle

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import okio.BufferedSink

/**
 * Writes spoken words as one track: each word starts [intervalSeconds] after the one before it, and
 * the silence that follows is the listener's time to picture it.
 *
 * Every word is evened out before it is written. Voices speak some words louder than others, and a
 * word that jumps out of the dark is the opposite of what the track is for; each one is brought to
 * the same moderate peak and eased in and out, so no slot starts or ends with a click. A word
 * longer than its slot is cut at the slot's end rather than pushing the rest of the track late.
 *
 * The track is written as it is built, slot by slot, so a long one never has to fit in memory twice.
 * Its rate is the first word's; a word spoken at another rate is converted to it.
 */
internal fun writeShuffleTrack(sink: BufferedSink, words: List<Pcm>, intervalSeconds: Int) {
    require(words.isNotEmpty()) { "A track needs at least one word." }
    require(intervalSeconds > 0) { "Words need time between them: $intervalSeconds" }
    val sampleRate = words.first().sampleRate
    val slot = sampleRate * intervalSeconds
    val leadIn = sampleRate * LEAD_IN_MS / MS_PER_SECOND

    Wav.writeHeader(sink, sampleRate, slot.toLong() * words.size)
    words.forEach { word ->
        val spoken = evenedOut(word.resampledTo(sampleRate).samples, maxLength = slot - leadIn)
        writeSilence(sink, leadIn)
        spoken.forEach { sink.writeShortLe(it.toInt()) }
        writeSilence(sink, slot - leadIn - spoken.size)
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

/** Brings [samples] to [TARGET_PEAK], cut to [maxLength], with short fades at both ends. */
private fun evenedOut(samples: ShortArray, maxLength: Int): ShortArray {
    val spoken = if (samples.size > maxLength) samples.copyOf(maxLength) else samples
    val peak = spoken.maxOfOrNull { abs(it.toInt()) } ?: 0
    if (peak == 0) return spoken
    val gain = TARGET_PEAK / peak
    val fade = min(spoken.size / 2, FADE_SAMPLES)
    return ShortArray(spoken.size) { index ->
        val edge = min(index, spoken.lastIndex - index)
        val envelope = if (edge < fade) edge.toDouble() / fade else 1.0
        (spoken[index] * gain * envelope).roundToInt().toShort()
    }
}

/** Most of a track is silence; it is written a block at a time rather than a sample at a time. */
private fun writeSilence(sink: BufferedSink, count: Int) {
    var remaining = count.toLong() * BYTES_PER_SAMPLE
    while (remaining > 0) {
        val block = min(remaining, SILENCE.size.toLong()).toInt()
        sink.write(SILENCE, 0, block)
        remaining -= block
    }
}

private const val BYTES_PER_SAMPLE = 2
private const val SILENCE_BLOCK_BYTES = 8_192
private val SILENCE = ByteArray(SILENCE_BLOCK_BYTES)

/** Half a second of quiet before each word, so the track does not open on a syllable. */
private const val LEAD_IN_MS = 500
private const val MS_PER_SECOND = 1_000

/** 0.4 of full scale, about 8 dB below it: clearly heard, never the loudest thing in a dark room. */
private const val TARGET_PEAK = 13_107.0

/**
 * The fade's length in samples. Counted in samples rather than time on purpose: a voice's rate is
 * between 16 and 48 kHz, which keeps it between 5 and 15 ms — long enough to hide the edge of a
 * slot, short enough not to soften the word.
 */
private const val FADE_SAMPLES = 240
