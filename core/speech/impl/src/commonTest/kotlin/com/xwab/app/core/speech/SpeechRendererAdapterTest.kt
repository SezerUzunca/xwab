@file:OptIn(ExperimentalKermitApi::class)

package com.xwab.app.core.speech

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import com.xwab.app.core.speech.port.NoiseBed
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class SpeechRendererAdapterTest {
    private val fileSystem = FakeFileSystem()

    @AfterTest
    fun closeFileSystem() {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }

    @Test
    fun aRequestBecomesOneTrackInItsFolder() = runTest {
        val voice = FakeVoice(fileSystem)

        val path = rendered(renderer(voice).render(request("kite", "cup")))

        assertEquals(listOf("kite", "cup"), voice.spoken)
        assertEquals(ROOT / "tracks" / "shuffle", path.parent)
        assertTrue(path.name.startsWith("deck.") && path.name.endsWith(".wav"), path.name)
        // Each word a tenth of a second per letter at 1 kHz, then a 0.5-second pause.
        assertEquals(400 + 500 + 300 + 500, decode(path).samples.size)
        assertEquals(1, voice.closed, "the voice is released once everything is spoken")
    }

    /**
     * Text is spoken once and kept; a request that should differ every time still gets a track of
     * its own, under a name of its own, and the name's earlier track goes.
     */
    @Test
    fun textIsSpokenOncePerVoiceAndOnlyTheNewestTrackIsKept() = runTest {
        val voice = FakeVoice(fileSystem)
        val renderer = renderer(voice)

        val first = rendered(renderer.render(request("kite", "cup")))
        val second = rendered(renderer.render(request("cup", "kite")))

        assertEquals(listOf("kite", "cup"), voice.spoken, "the second render spoke nothing again")
        assertNotEquals(first, second, "a player handed its current address may keep the old audio")
        assertEquals(listOf(second), fileSystem.list(ROOT / "tracks" / "shuffle"))
    }

    /** Fixed content made once is handed back as it is; a change to it makes a new track. */
    @Test
    fun aReusableTrackIsMadeOnceUntilItsContentChanges() = runTest {
        val voice = FakeVoice(fileSystem)
        val renderer = renderer(voice)
        val guide = request("breathe", reuse = true)

        val first = rendered(renderer.render(guide))
        val again = rendered(renderer.render(guide))
        val changed = rendered(renderer.render(guide.copy(bed = NoiseBed(level = 0.03, fadeSeconds = 1.0))))

        assertEquals(first, again)
        assertNotEquals(first, changed)
        assertEquals(listOf("breathe"), voice.spoken)
        assertEquals(listOf(changed), fileSystem.list(ROOT / "tracks" / "shuffle"))
    }

    /** One name's tracks are never taken for another's, however the names begin. */
    @Test
    fun tracksOfSimilarNamesLeaveEachOtherAlone() = runTest {
        val renderer = renderer(FakeVoice(fileSystem))

        val beach = rendered(renderer.render(request("kite", name = "beach")))
        val evening = rendered(renderer.render(request("kite", name = "beach-evening")))
        rendered(renderer.render(request("cup", name = "beach-evening")))

        assertTrue(fileSystem.exists(beach))
        assertFalse(fileSystem.exists(evening))
    }

    @Test
    fun anotherVoiceSpeaksTheTextItself() = runTest {
        renderer(FakeVoice(fileSystem, voiceId = "com.engine:en-us-a")).render(request("kite"))
        val other = FakeVoice(fileSystem, voiceId = "com.engine:en-us-b")

        renderer(other).render(request("kite"))

        assertEquals(listOf("kite"), other.spoken)
        assertEquals(2, fileSystem.list(ROOT / "utterances").size)
    }

    /** No voice for the language is worth another try once one is installed, and a log line. */
    @Test
    fun aDeviceWithoutTheVoiceIsUnavailableAndLogged() = runTest {
        val log = TestLogWriter(loggable = Severity.Verbose)

        val result = renderer(FakeVoice(fileSystem, available = false), recordingLogger(log)).render(request("kite"))

        assertIs<SpeechResult.Unavailable>(result)
        assertEquals(Severity.Warn, log.logs.single().severity)
        assertFalse(fileSystem.exists(ROOT / "tracks"))
    }

    /** Speech the voice left unreadable fails one render, not every render after it. */
    @Test
    fun unreadableSpeechIsSpokenAgainNextTime() = runTest {
        val voice = FakeVoice(fileSystem)
        val renderer = renderer(voice)
        rendered(renderer.render(request("kite")))
        val utterances = fileSystem.listRecursively(ROOT / "utterances").filter { it.name.endsWith(".wav") }.toList()
        fileSystem.write(utterances.single()) { writeUtf8("not a wav") }

        assertIs<SpeechResult.Unavailable>(renderer.render(request("kite")))
        rendered(renderer.render(request("kite")))

        assertEquals(listOf("kite", "kite"), voice.spoken)
    }

    private fun TestScope.renderer(voice: Voice, logger: Logger = SilentLogger) = SpeechRendererAdapter(
        voice = voice,
        location = { ROOT },
        fileSystem = fileSystem,
        fileDispatcher = StandardTestDispatcher(testScheduler),
        random = Random(SEED),
        logger = logger,
    )

    private fun request(vararg words: String, name: String = "deck", reuse: Boolean = false) = SpeechRequest(
        folder = "shuffle",
        name = name,
        languageTag = "en-US",
        segments = words.flatMap { listOf(SpeechSegment.Spoken(it), SpeechSegment.Pause(0.5)) },
        reuse = reuse,
    )

    private fun rendered(result: SpeechResult): Path = assertIs<SpeechResult.Rendered>(result).path.toPath()

    private fun decode(path: Path) = Wav.decode(fileSystem.read(path) { readByteArray() })

    private companion object {
        val ROOT: Path = "/cache/speech".toPath()
        const val SEED = 7
    }
}
