package com.xwab.app.core.speech

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.xwab.app.core.speech.port.NoiseBed
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.createGraphFactory
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assume.assumeTrue

/**
 * The Android production graph, with the Context and the text-to-speech engine only a device has.
 *
 * The speaking tests need a text-to-speech engine, as the app does. Phones ship one, and so do the
 * emulator images with Google APIs; CI's plain AOSP image may not, and there they are reported as
 * skipped rather than passed. An engine without an English voice fails them, saying so.
 */
class AndroidSpeechGraphTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cache: File get() = context.cacheDir.resolve("speech")

    @AfterTest
    fun removeWhatTheTestsWrote() {
        cache.deleteRecursively()
    }

    /** The app graph may build this graph on the main thread; building it starts no engine and reads no directory. */
    @Test
    fun theModuleGraphHandsOutOneRenderer() {
        val graph = createGraphFactory<AndroidSpeechGraph.Factory>().create(context)

        assertSame(graph.speech, graph.speech)
    }

    /** The device's own engine writes speech as WAV this module reads back. */
    @Test
    fun theDevicesVoiceSpeaksTextThisModuleCanRead() = runBlocking {
        assumeAnEngine()
        val target = cache.resolve("kite.wav").also { it.parentFile?.mkdirs() }

        AndroidVoice(context).open("en-US").use { voice -> voice.speak("kite", target.absolutePath.toPath()) }

        val spoken = Wav.decode(target.readBytes())
        val shortest = spoken.sampleRate / TENTHS_PER_SECOND
        assertTrue(spoken.samples.size > shortest, "a tenth of a second at least: ${spoken.samples.size}")
        assertTrue(spoken.samples.any { it != 0.toShort() }, "the speech is not silence")
    }

    /** Nothing replaced: the graph speaks a request with a bed into a track on disk, and reuses it. */
    @Test
    fun theUnmodifiedGraphRendersAndReusesATrack() = runBlocking {
        assumeAnEngine()
        val speech = createGraphFactory<AndroidSpeechGraph.Factory>().create(context).speech
        val request = SpeechRequest(
            folder = "test",
            name = "rest",
            languageTag = "en-US",
            segments = listOf(SpeechSegment.Spoken("Let your shoulders drop."), SpeechSegment.Pause(2.0)),
            bed = NoiseBed(level = 0.03, fadeSeconds = 1.0),
            reuse = true,
        )

        val first = assertIs<SpeechResult.Rendered>(speech.render(request))
        val again = assertIs<SpeechResult.Rendered>(speech.render(request))

        assertEquals(first.path, again.path)
        val track = Wav.decode(File(first.path).readBytes())
        assertTrue(track.samples.size > 2 * track.sampleRate, "the speech and the pause: ${track.samples.size}")
    }

    private fun assumeAnEngine() = assumeTrue(
        "This device has no text-to-speech engine.",
        context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            .isNotEmpty(),
    )

    private companion object {
        const val TENTHS_PER_SECOND = 10
    }
}
