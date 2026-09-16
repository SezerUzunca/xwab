@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.playback.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.*
import platform.Foundation.*

/**
 * Drives [IosPlaybackEngine] against a real asset on the simulator.
 *
 * The engine's hardest paths are the ones AVFoundation only reaches with an actual item: readiness,
 * the looper that may only be built once a duration is known, and a queue that empties itself when a
 * non-looping item finishes. A silent WAV is written to the temporary directory for each test rather
 * than shipped as a resource, so the file is small, disposable, and exact about its duration.
 *
 * Kotlin/Native tests own the main thread, and every engine callback arrives through the main run
 * loop — notifications and the observation timer alike — so waiting means spinning that run loop
 * rather than blocking it. A failure here is read back from the JUnit report the workflow turns
 * into annotations, so each expectation is its own test and carries what the engine could see.
 */
class IosPlaybackEngineAudioTest {

    @Test
    fun theFixtureIsAFileOnDiskOfTheExpectedSize() {
        val path = writeSilentWav(seconds = 1.0)

        assertTrue(
            NSFileManager.defaultManager.fileExistsAtPath(path),
            "The fixture was never written: $path",
        )
        val size = NSFileManager.defaultManager.contentsAtPath(path)?.length
        assertTrue(
            size == (WAV_HEADER_BYTES + 8_000 * BYTES_PER_FRAME).toULong(),
            "The fixture is $size bytes, not a 1-second 8kHz mono PCM WAV.",
        )
    }

    @Test
    fun theTestRunsWhereTheEngineDeliversItsCallbacks() {
        // Every engine callback is posted to the main queue, and its observation timer is scheduled
        // on the run loop of whichever thread loaded the item. Both only turn if this test owns the
        // main thread, so the rest of this class is meaningless without it.
        assertTrue(NSThread.isMainThread, "Simulator tests are not running on the main thread.")
    }

    @Test
    fun aLoopingItemBecomesReadyWithoutBlockingTheCaller() {
        val engine = engine()
        val path = writeSilentWav(seconds = 1.0)

        val accepted = engine.load(path, looping = true, operationId = 1L)

        assertTrue(accepted, "The engine refused a file URL it should accept.")
        assertTrue(
            spinUntil { engine.isReadyToPlay },
            "A looping item never became ready. ${diagnosis(engine)}",
        )
        engine.release()
    }

    @Test
    fun aLoopingItemReportsNoLoopFailureOnceItIsReady() {
        val engine = engine()

        engine.load(writeSilentWav(seconds = 1.0), looping = true, operationId = 1L)
        spinUntil { engine.isReadyToPlay }

        assertFalse(
            engine.loopErrorMessage != null,
            "A playable looping item reported a loop failure.",
        )
        engine.release()
    }

    @Test
    fun aZeroDurationLoopingItemFailsInsteadOfReachingTheLooper() {
        val engine = engine()

        engine.load(writeSilentWav(seconds = 0.0), looping = true, operationId = 1L)

        assertTrue(
            spinUntil { engine.loopErrorMessage != null || engine.hasItemFailure },
            "Zero-duration looping media neither failed nor became ready. ${diagnosis(engine)}",
        )
        engine.release()
    }

    @Test
    fun playingAShortItemToItsEndIsReported() {
        var endedOperationId: Long? = null
        val engine = engine(onPlaybackEnded = { endedOperationId = it })
        activateAudioSession()

        engine.load(writeSilentWav(seconds = 0.4), looping = false, operationId = 7L)
        spinUntil { engine.isReadyToPlay }
        engine.play()

        assertTrue(
            spinUntil(timeoutSeconds = 20.0) { endedOperationId != null },
            "Playback never reported reaching the end of the item. ${diagnosis(engine)}",
        )
        engine.release()
    }

    @Test
    fun aFinishedItemIsQueuedAgainWhenPlaybackRestarts() {
        var ended = false
        val engine = engine(onPlaybackEnded = { ended = true })
        activateAudioSession()

        engine.load(writeSilentWav(seconds = 0.4), looping = false, operationId = 7L)
        spinUntil { engine.isReadyToPlay }
        engine.play()
        spinUntil(timeoutSeconds = 20.0) { ended }

        // The queue may already be empty here: AVQueuePlayer removes an item it has played to the
        // end. Restarting has to rebuild it from the source rather than seek within nothing.
        var restarted = false
        engine.seekTo(0L) { restarted = it }

        assertTrue(
            spinUntil(timeoutSeconds = 20.0) { restarted && engine.hasCurrentItem },
            "A finished item could not be queued again. ${diagnosis(engine)}",
        )
        engine.release()
    }

    private fun engine(
        onPlaybackEnded: (Long) -> Unit = {},
    ) = IosPlaybackEngine(
        onStateChanged = {},
        onPlaybackEnded = onPlaybackEnded,
        onPlaybackFailed = { _, _ -> },
        onReadinessTimedOut = {},
    )

    /**
     * What the engine could see when an expectation ran out of patience. Nothing here is an
     * expectation of its own; it is what turns "never became ready" into something a run that
     * cannot be attached to can still be read from.
     */
    private fun diagnosis(engine: IosPlaybackEngine): String = listOf(
        "mainThread=${NSThread.isMainThread}",
        "hasCurrentItem=${engine.hasCurrentItem}",
        "isReadyToPlay=${engine.isReadyToPlay}",
        "isWaitingToPlay=${engine.isWaitingToPlay}",
        "isPlaying=${engine.isPlaying}",
        "hasItemFailure=${engine.hasItemFailure}",
        "itemError=${engine.itemErrorMessage}",
        "loopError=${engine.loopErrorMessage}",
        "durationMs=${engine.durationMs()}",
    ).joinToString(separator = " ")

    private fun activateAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    /** Spins the main run loop until [condition] holds, so engine callbacks can be delivered. */
    private fun spinUntil(
        timeoutSeconds: Double = 10.0,
        condition: () -> Boolean,
    ): Boolean {
        val startedAt = TimeSource.Monotonic.markNow()
        while (!condition()) {
            if (startedAt.elapsedNow().inWholeMilliseconds > (timeoutSeconds * 1_000).toLong()) {
                return false
            }
            NSRunLoop.mainRunLoop.runUntilDate(
                NSDate().dateByAddingTimeInterval(RUN_LOOP_SLICE_SECONDS),
            )
        }
        return true
    }

    /**
     * Writes a mono 16-bit PCM WAV of [seconds] of silence and returns its absolute path. Silence is
     * enough: these tests are about the engine's state machine, not about what comes out of it.
     */
    private fun writeSilentWav(seconds: Double): String {
        val sampleRate = 8_000
        val dataBytes = (sampleRate * seconds).toInt() * BYTES_PER_FRAME
        val bytes = ByteArray(WAV_HEADER_BYTES + dataBytes)

        fun putAscii(offset: Int, text: String) {
            text.forEachIndexed { index, character -> bytes[offset + index] = character.code.toByte() }
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
        const val RUN_LOOP_SLICE_SECONDS = 0.02
    }
}
