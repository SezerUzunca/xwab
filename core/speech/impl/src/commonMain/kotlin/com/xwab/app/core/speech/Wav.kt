package com.xwab.app.core.speech

import okio.Buffer
import okio.BufferedSink
import okio.IOException

/**
 * Mono 16-bit audio: what a voice speaks one word into, and what the word track is made of.
 *
 * Both platforms' voices hand over 16-bit linear PCM, and it is also what both players open without
 * a decoder of their own, so nothing in this module converts between sample formats beyond that.
 */
internal class Pcm(val sampleRate: Int, val samples: ShortArray) {
    init {
        require(sampleRate > 0) { "A sample rate must be positive: $sampleRate" }
    }
}

/**
 * The one container this module reads and writes: RIFF WAVE holding 16-bit linear PCM.
 *
 * A voice writes one file per word and the renderer writes the track, and both players open WAV
 * from a local path, so there is no encoder to carry. The cost is size, which the renderer bounds
 * by how many words one track holds.
 */
internal object Wav {
    /** Writes a mono 16-bit header for [sampleCount] samples, which the caller then writes itself. */
    fun writeHeader(sink: BufferedSink, sampleRate: Int, sampleCount: Long) {
        val dataBytes = sampleCount * BYTES_PER_SAMPLE
        require(dataBytes <= MAX_DATA_BYTES) { "A WAV file holds at most $MAX_DATA_BYTES bytes of audio." }
        sink.writeUtf8(RIFF)
        sink.writeIntLe((RIFF_HEADER_BYTES + dataBytes).toInt())
        sink.writeUtf8(WAVE)
        sink.writeUtf8(FORMAT_CHUNK)
        sink.writeIntLe(PCM_FORMAT_CHUNK_BYTES)
        sink.writeShortLe(PCM_FORMAT)
        sink.writeShortLe(MONO)
        sink.writeIntLe(sampleRate)
        sink.writeIntLe(sampleRate * BYTES_PER_SAMPLE)
        sink.writeShortLe(BYTES_PER_SAMPLE)
        sink.writeShortLe(BITS_PER_SAMPLE)
        sink.writeUtf8(DATA_CHUNK)
        sink.writeIntLe(dataBytes.toInt())
    }

    fun encode(pcm: Pcm): ByteArray {
        val buffer = Buffer()
        writeHeader(buffer, pcm.sampleRate, pcm.samples.size.toLong())
        pcm.samples.forEach { buffer.writeShortLe(it.toInt()) }
        return buffer.readByteArray()
    }

    /**
     * Reads 16-bit PCM, mixing any channels down to one.
     *
     * Chunks other than the format and the audio are skipped, since a writer may add its own. A
     * data chunk whose declared size is zero or runs past the end of the file is read to the end:
     * a writer that streams the audio can only fill in the size afterwards, and not every one does.
     *
     * @throws IOException when the bytes are not 16-bit PCM WAV.
     */
    fun decode(bytes: ByteArray): Pcm {
        val source = Buffer().write(bytes)
        ensure(source.size >= MIN_FILE_BYTES && source.readUtf8(CHUNK_ID_BYTES) == RIFF) { "Not a RIFF file." }
        source.skip(CHUNK_SIZE_BYTES)
        ensure(source.readUtf8(CHUNK_ID_BYTES) == WAVE) { "Not a WAVE file." }

        var format: Format? = null
        var audio: Pcm? = null
        while (audio == null && source.size >= CHUNK_HEADER_BYTES) {
            val id = source.readUtf8(CHUNK_ID_BYTES)
            val declared = source.readIntLe().toLong() and UNSIGNED_INT_MASK
            when (id) {
                FORMAT_CHUNK -> format = readFormat(source, declared)
                DATA_CHUNK -> {
                    ensure(format != null) { "Audio before its format." }
                    val size = if (declared == 0L || declared > source.size) source.size else declared
                    audio = readSamples(source, size, requireNotNull(format))
                }
                else -> source.skip(minOf(declared + declared % 2, source.size))
            }
        }
        return audio ?: throw IOException("No audio in the file.")
    }

    private fun readFormat(source: Buffer, size: Long): Format {
        ensure(size >= PCM_FORMAT_CHUNK_BYTES) { "A format chunk of $size bytes." }
        val tag = source.readShortLe().toInt() and UNSIGNED_SHORT_MASK
        val channels = source.readShortLe().toInt()
        val sampleRate = source.readIntLe()
        source.skip(BYTE_RATE_AND_BLOCK_ALIGN_BYTES)
        val bits = source.readShortLe().toInt()
        source.skip(minOf(size - PCM_FORMAT_CHUNK_BYTES + size % 2, source.size))
        ensure(tag == PCM_FORMAT || tag == EXTENSIBLE_FORMAT) { "Format $tag is not PCM." }
        ensure(bits == BITS_PER_SAMPLE) { "$bits-bit audio; only $BITS_PER_SAMPLE-bit is read." }
        ensure(channels >= 1 && sampleRate > 0) { "$channels channels at $sampleRate Hz." }
        return Format(channels, sampleRate)
    }

    /** [check], with the I/O failure a malformed file is rather than a programming error. */
    private inline fun ensure(condition: Boolean, message: () -> String) {
        if (!condition) throw IOException(message())
    }

    private fun readSamples(source: Buffer, size: Long, format: Format): Pcm {
        val frames = (size / (BYTES_PER_SAMPLE * format.channels)).toInt()
        val samples = ShortArray(frames) {
            var sum = 0
            repeat(format.channels) { sum += source.readShortLe() }
            (sum / format.channels).toShort()
        }
        return Pcm(format.sampleRate, samples)
    }

    private class Format(val channels: Int, val sampleRate: Int)

    private const val RIFF = "RIFF"
    private const val WAVE = "WAVE"
    private const val FORMAT_CHUNK = "fmt "
    private const val DATA_CHUNK = "data"
    private const val CHUNK_ID_BYTES = 4L
    private const val CHUNK_SIZE_BYTES = 4L
    private const val CHUNK_HEADER_BYTES = CHUNK_ID_BYTES + CHUNK_SIZE_BYTES

    /** "RIFF", its size and "WAVE": the least a file can be before its first chunk. */
    private const val MIN_FILE_BYTES = CHUNK_HEADER_BYTES + CHUNK_ID_BYTES

    /** What the RIFF size counts besides the audio: "WAVE", the format chunk and the data header. */
    private const val RIFF_HEADER_BYTES = 36

    private const val PCM_FORMAT_CHUNK_BYTES = 16
    private const val PCM_FORMAT = 1
    private const val EXTENSIBLE_FORMAT = 0xFFFE
    private const val MONO = 1
    private const val BITS_PER_SAMPLE = 16
    private const val BYTES_PER_SAMPLE = 2
    private const val BYTE_RATE_AND_BLOCK_ALIGN_BYTES = 6L
    private const val UNSIGNED_INT_MASK = 0xFFFF_FFFFL
    private const val UNSIGNED_SHORT_MASK = 0xFFFF

    /** The RIFF size field is 32 bits and counts the header too. */
    private const val MAX_DATA_BYTES = Int.MAX_VALUE.toLong() - RIFF_HEADER_BYTES
}
