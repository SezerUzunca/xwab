package com.xwab.app.core.shuffle

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.IOException

class WavTest {
    @Test
    fun whatIsWrittenReadsBackUnchanged() {
        val pcm = Pcm(24_000, shortArrayOf(0, 1, -1, Short.MAX_VALUE, Short.MIN_VALUE))

        val read = Wav.decode(Wav.encode(pcm))

        assertEquals(24_000, read.sampleRate)
        assertContentEquals(pcm.samples, read.samples)
    }

    /** The header both players read: 44 bytes, then two bytes a sample. */
    @Test
    fun theHeaderDescribesMono16BitPcm() {
        val bytes = Wav.encode(Pcm(22_050, ShortArray(3)))

        assertEquals(44 + 6, bytes.size)
        val header = Buffer().write(bytes)
        assertEquals("RIFF", header.readUtf8(4))
        assertEquals(36 + 6, header.readIntLe())
        assertEquals("WAVEfmt ", header.readUtf8(8))
        assertEquals(16, header.readIntLe())
        assertEquals(1, header.readShortLe().toInt(), "linear PCM")
        assertEquals(1, header.readShortLe().toInt(), "mono")
        assertEquals(22_050, header.readIntLe())
        assertEquals(44_100, header.readIntLe(), "bytes a second")
        assertEquals(2, header.readShortLe().toInt(), "bytes a frame")
        assertEquals(16, header.readShortLe().toInt(), "bits a sample")
        assertEquals("data", header.readUtf8(4))
        assertEquals(6, header.readIntLe())
    }

    /** A writer may add chunks of its own and write stereo; the audio is still found, mixed down. */
    @Test
    fun otherChunksAreSkippedAndChannelsMixedDown() {
        val bytes = wav(channels = 2, sampleRate = 16_000, extraChunk = true, frames = listOf(100 to 300, -50 to 50))

        val read = Wav.decode(bytes)

        assertEquals(16_000, read.sampleRate)
        assertContentEquals(shortArrayOf(200, 0), read.samples)
    }

    /** A streaming writer that never went back to fill in the size: the audio runs to the end. */
    @Test
    fun aDataChunkWithoutItsSizeIsReadToTheEnd() {
        val bytes = wav(channels = 1, sampleRate = 8_000, frames = listOf(7 to 0, 8 to 0, 9 to 0), dataSize = 0)

        assertContentEquals(shortArrayOf(7, 8, 9), Wav.decode(bytes).samples)
    }

    @Test
    fun anythingButSixteenBitPcmIsRefused() {
        assertFailsWith<IOException> { Wav.decode("not audio at all".encodeToByteArray()) }
        assertFailsWith<IOException> { Wav.decode(wav(channels = 1, sampleRate = 8_000, frames = SILENT, bits = 8)) }
        assertFailsWith<IOException> { Wav.decode(wav(channels = 1, sampleRate = 8_000, frames = SILENT, tag = 3)) }
        assertFailsWith<IOException> { Wav.decode(Wav.encode(Pcm(8_000, ShortArray(4))).copyOf(20)) }
    }

    private companion object {
        val SILENT = listOf(0 to 0)
    }

    /** Hand-written, so the reader is tested against bytes it did not write itself. */
    @Suppress("LongParameterList")
    private fun wav(
        channels: Int,
        sampleRate: Int,
        frames: List<Pair<Int, Int>>,
        extraChunk: Boolean = false,
        dataSize: Int = frames.size * channels * 2,
        bits: Int = 16,
        tag: Int = 1,
    ): ByteArray {
        val body = Buffer()
        body.writeUtf8("WAVE")
        if (extraChunk) body.writeUtf8("LIST").writeIntLe(3).writeUtf8("abc").writeByte(0)
        body.writeUtf8("fmt ").writeIntLe(16).writeShortLe(tag).writeShortLe(channels).writeIntLe(sampleRate)
            .writeIntLe(sampleRate * channels * 2).writeShortLe(channels * 2).writeShortLe(bits)
        body.writeUtf8("data").writeIntLe(dataSize)
        frames.forEach { (left, right) ->
            body.writeShortLe(left)
            if (channels == 2) body.writeShortLe(right)
        }
        val file = Buffer().writeUtf8("RIFF").writeIntLe(body.size.toInt())
        file.writeAll(body)
        return file.readByteArray()
    }
}
