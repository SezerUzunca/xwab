package com.xwab.app.core.shuffle

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class ShuffleTrackRendererTest {
    private val fileSystem = FakeFileSystem()

    @AfterTest
    fun closeFileSystem() {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }

    @Test
    fun aTrackSpeaksAFreshSelectionWithoutRepeatingAWord() = runTest {
        val voice = FakeVoice(fileSystem)

        val track = renderer(voice).render(deck(intervalSeconds = 3), wordList(wordsPerTrack = 3))

        assertEquals(3, voice.spoken.size)
        assertEquals(voice.spoken.toSet().size, voice.spoken.size, "no word twice in one track")
        assertTrue(voice.spoken.all { it in wordList().words })
        assertEquals(ROOT / "tracks", track.parent)
        assertTrue(track.name.startsWith("$TEST_DECK-") && track.name.endsWith(".wav"), track.name)
        assertEquals(3 * 3 * 1_000, Wav.decode(fileSystem.read(track) { readByteArray() }).samples.size)
        assertEquals(1, voice.closed, "the voice is released once the words are spoken")
    }

    /**
     * Words are spoken once and kept; each render still draws its own order and writes its own
     * track, under a name of its own, and the deck's earlier track goes.
     */
    @Test
    fun wordsAreSpokenOncePerVoiceAndOnlyTheNewestTrackIsKept() = runTest {
        val voice = FakeVoice(fileSystem)
        val renderer = renderer(voice)
        val everyWord = wordList(wordsPerTrack = 4)

        val first = renderer.render(deck(), everyWord)
        val second = renderer.render(deck(), everyWord)

        assertEquals(everyWord.words.sorted(), voice.spoken.sorted(), "the second render spoke nothing again")
        assertNotEquals(first, second, "a player handed its current address may keep the old audio")
        assertEquals(listOf(second), fileSystem.list(ROOT / "tracks"))
    }

    @Test
    fun anotherVoiceSpeaksTheWordsItself() = runTest {
        renderer(FakeVoice(fileSystem, voiceId = "com.engine:en-us-a")).render(deck(), wordList(wordsPerTrack = 4))
        val other = FakeVoice(fileSystem, voiceId = "com.engine:en-us-b")

        renderer(other).render(deck(), wordList(wordsPerTrack = 4))

        assertEquals(4, other.spoken.size)
        assertTrue(fileSystem.exists(ROOT / "words" / "com-engine-en-us-a" / "kite.wav"))
        assertTrue(fileSystem.exists(ROOT / "words" / "com-engine-en-us-b" / "kite.wav"))
    }

    @Test
    fun aDeviceWithoutTheVoiceCannotRender() = runTest {
        assertFailsWith<ShuffleRenderException> {
            renderer(FakeVoice(fileSystem, available = false)).render(deck(), wordList())
        }
        assertFalse(fileSystem.exists(ROOT / "tracks"))
    }

    /** A word file the voice left unreadable fails one render, not every render after it. */
    @Test
    fun anUnreadableSpokenWordIsSpokenAgainNextTime() = runTest {
        val voice = FakeVoice(fileSystem)
        val broken = ROOT / "words" / "test-voice" / "kite.wav"
        fileSystem.createDirectories(broken.parent!!)
        fileSystem.write(broken) { writeUtf8("not a wav") }
        val renderer = renderer(voice)

        assertFailsWith<ShuffleRenderException> { renderer.render(deck(), wordList(wordsPerTrack = 4)) }
        assertFalse(fileSystem.exists(broken))

        renderer.render(deck(), wordList(wordsPerTrack = 4))
        assertTrue("kite" in voice.spoken)
    }

    private fun TestScope.renderer(voice: WordVoice) = ShuffleTrackRenderer(
        voice = voice,
        location = { ROOT },
        fileSystem = fileSystem,
        fileDispatcher = StandardTestDispatcher(testScheduler),
        random = Random(SEED),
    )

    private companion object {
        val ROOT: Path = "/cache/shuffle".toPath()
        const val SEED = 7
    }
}
