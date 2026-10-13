package com.xwab.app.core.shuffle

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.TestConfig
import co.touchlab.kermit.TestLogWriter
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import okio.FileSystem
import okio.Path

/** A logger that writes nowhere, for the tests that do not read what was logged. */
internal val SilentLogger = Logger(StaticConfig(logWriterList = emptyList()))

/** A logger over [writer], for the tests that read what was logged. */
@OptIn(ExperimentalKermitApi::class)
internal fun recordingLogger(writer: TestLogWriter): Logger =
    Logger(TestConfig(minSeverity = Severity.Verbose, logWriterList = listOf(writer)))

internal fun deck(id: String = TEST_DECK, intervalSeconds: Int = 3) = ShuffleDeck(
    id = ShuffleDeckId(id),
    title = "Test Deck",
    description = "Words for a test.",
    languageTag = "en-US",
    wordIntervalSeconds = intervalSeconds,
)

internal fun wordList(
    words: List<String> = listOf("kite", "cup", "owl", "pear"),
    wordsPerTrack: Int = 3,
    deckId: String = TEST_DECK,
) = ShuffleWordList(deckId = deckId, wordsPerTrack = wordsPerTrack, words = words)

internal const val TEST_DECK = "test-deck"

/**
 * A voice that writes each word as a short tone into [fileSystem], and remembers what it was asked.
 *
 * The tone is a word's length in tenths of a second at [sampleRate], at a level the track writer
 * has to bring up, so a test can tell a written word from silence.
 */
internal class FakeVoice(
    private val fileSystem: FileSystem,
    private val voiceId: String = "test-voice",
    private val sampleRate: Int = 1_000,
    private val available: Boolean = true,
) : WordVoice {
    val spoken = mutableListOf<String>()
    var opened = 0
        private set
    var closed = 0
        private set

    override suspend fun open(languageTag: String): VoiceSession {
        if (!available) throw ShuffleRenderException("No $languageTag voice.")
        opened++
        return object : VoiceSession {
            override val voiceId: String = this@FakeVoice.voiceId

            override suspend fun speak(word: String, target: Path) {
                spoken += word
                val tone = Pcm(sampleRate, ShortArray(word.length * sampleRate / 10) { WORD_LEVEL })
                fileSystem.write(target) { write(Wav.encode(tone)) }
            }

            override fun close() {
                closed++
            }
        }
    }

    companion object {
        const val WORD_LEVEL: Short = 1_000
    }
}
