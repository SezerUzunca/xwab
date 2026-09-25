@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.playback.platform

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

/**
 * Drives [IosPlaybackEngine] against a real file, as far as this environment allows.
 *
 * It does not allow much. `iosSimulatorArm64Test` runs a bare Kotlin/Native binary under the
 * simulator rather than an app, and AVFoundation will not open media for it: a plain `AVPlayer`
 * given a valid PCM WAV — written here, read back here, and verified header and all — reports
 * `AVPlayerItemStatusFailed` with "The operation could not be completed". An `AVQueuePlayer` then
 * discards the failed item and empties its queue, which is why an engine driven this way looks like
 * it lost its item and has no error to show for it: every flag it publishes is derived from
 * `player.currentItem`, and there is no longer an item to derive them from.
 *
 * So everything up to the decoder is testable here and nothing past it is. Readiness, looping,
 * playing to the end and re-queueing a finished item need a device or a simulator running a real
 * app; they are listed in the pull request as verification this change has not had.
 *
 * A source that cannot be opened is the exception, because it fails the same way everywhere: a
 * file of bytes that are not audio is refused on a device as well as here. It is also the path the
 * engine once went quiet on — the queue dropped the failed item, and the failure went with it — so
 * it is tested with such a file rather than with the WAV, which only fails because of where it runs.
 */
class IosPlaybackEngineAudioTest {

    @Test
    fun loadingAnItemAttachesItToThePlayerAtOnce() {
        // The queue is populated synchronously, before the asset is opened, so this holds even
        // where nothing can be decoded — and it is what tells an empty queue apart from an item
        // that never became ready.
        val engine = IosPlaybackEngine(
            onStateChanged = {},
            onPlaybackEnded = {},
            onPlaybackFailed = { _, _ -> },
            onReadinessTimedOut = {},
        )

        val accepted = engine.load(writeSilentWav(), looping = false, operationId = 1L)

        assertTrue(accepted, "The engine refused a file URL it should accept.")
        assertTrue(engine.hasCurrentItem, "load() attached nothing to the player.")
        engine.release()
    }

    @Test
    fun aSourceThatCannotBeOpenedIsReportedAsAFailure() {
        assertFailureReported(looping = false)
    }

    @Test
    fun aLoopingSourceThatCannotBeOpenedIsReportedAsAFailure() {
        assertFailureReported(looping = true)
    }

    /**
     * Waits for the engine to report the failure through its state callback, which is the only way
     * the facade learns of it, and inside the readiness deadline, so it is the failure being reported
     * and not a load that merely ran out of time.
     */
    private fun assertFailureReported(looping: Boolean) {
        lateinit var engine: IosPlaybackEngine
        var failureReported = false
        var readinessTimedOut = false
        engine = IosPlaybackEngine(
            onStateChanged = { if (engine.hasItemFailure) failureReported = true },
            onPlaybackEnded = {},
            onPlaybackFailed = { _, _ -> },
            onReadinessTimedOut = { readinessTimedOut = true },
        )

        engine.load(writeUnplayableFile(), looping = looping, operationId = 1L)
        spinUntil(timeoutSeconds = 10.0) { failureReported || readinessTimedOut }

        assertTrue(failureReported, "The failure never reached the state callback.")
        engine.release()
    }

    /** Spins the main run loop until [condition] holds, so engine callbacks can be delivered. */
    private fun spinUntil(timeoutSeconds: Double, condition: () -> Boolean) {
        val startedAt = TimeSource.Monotonic.markNow()
        while (!condition() && startedAt.elapsedNow().inWholeMilliseconds < timeoutSeconds * 1_000) {
            NSRunLoop.mainRunLoop.runUntilDate(
                NSDate().dateByAddingTimeInterval(RUN_LOOP_SLICE_SECONDS),
            )
        }
    }

    /** Writes bytes that no platform decodes as audio, under an audio extension. */
    private fun writeUnplayableFile(): String {
        val bytes = ByteArray(4_096) { index -> (index * 31 + 7).toByte() }
        val path = NSTemporaryDirectory() + "xwab-unplayable-" + NSUUID().UUIDString() + ".mp3"
        val contents = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        NSFileManager.defaultManager.createFileAtPath(path, contents, null)
        return path
    }

    /** Writes one second of silence as a mono 16-bit PCM WAV and returns its absolute path. */
    private fun writeSilentWav(): String {
        val sampleRate = 8_000
        val dataBytes = sampleRate * BYTES_PER_FRAME
        val bytes = ByteArray(WAV_HEADER_BYTES + dataBytes)

        fun putAscii(offset: Int, text: String) {
            text.forEachIndexed { index, character ->
                bytes[offset + index] = character.code.toByte()
            }
        }

        fun putLittleEndian(offset: Int, value: Int, width: Int) {
            for (index in 0 until width) {
                bytes[offset + index] = ((value shr (8 * index)) and 0xFF).toByte()
            }
        }

        putAscii(0, "RIFF")
        putLittleEndian(4, WAV_HEADER_BYTES - 8 + dataBytes, 4)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putLittleEndian(16, 16, 4)
        putLittleEndian(20, 1, 2)
        putLittleEndian(22, 1, 2)
        putLittleEndian(24, sampleRate, 4)
        putLittleEndian(28, sampleRate * BYTES_PER_FRAME, 4)
        putLittleEndian(32, BYTES_PER_FRAME, 2)
        putLittleEndian(34, 16, 2)
        putAscii(36, "data")
        putLittleEndian(40, dataBytes, 4)

        val path = NSTemporaryDirectory() + "xwab-playback-" + NSUUID().UUIDString() + ".wav"
        val contents = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        NSFileManager.defaultManager.createFileAtPath(path, contents, null)
        return path
    }

    private companion object {
        const val WAV_HEADER_BYTES = 44
        const val BYTES_PER_FRAME = 2
        const val RUN_LOOP_SLICE_SECONDS = 0.05
    }
}
