package com.xwab.app.core.speech

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.TestConfig
import co.touchlab.kermit.TestLogWriter
import okio.FileSystem
import okio.Path

/** A logger that writes nowhere, for the tests that do not read what was logged. */
internal val SilentLogger = Logger(StaticConfig(logWriterList = emptyList()))

/** A logger over [writer], for the tests that read what was logged. */
@OptIn(ExperimentalKermitApi::class)
internal fun recordingLogger(writer: TestLogWriter): Logger =
    Logger(TestConfig(minSeverity = Severity.Verbose, logWriterList = listOf(writer)))

/**
 * A voice that writes each text as a steady tone into [fileSystem], and remembers what it was asked.
 *
 * The tone lasts a tenth of a second per character at [sampleRate], at a level the track writer has
 * to bring up, so a test can tell speech from silence and predict its length.
 */
internal class FakeVoice(
    private val fileSystem: FileSystem,
    private val voiceId: String = "test-voice",
    private val sampleRate: Int = 1_000,
    private val available: Boolean = true,
) : Voice {
    val spoken = mutableListOf<String>()
    var closed = 0
        private set

    override suspend fun open(languageTag: String): VoiceSession {
        if (!available) throw SpeechException("No $languageTag voice.")
        return object : VoiceSession {
            override val voiceId: String = this@FakeVoice.voiceId

            override suspend fun speak(text: String, target: Path) {
                spoken += text
                val tone = Pcm(sampleRate, ShortArray(text.length * sampleRate / 10) { LEVEL })
                fileSystem.write(target) { write(Wav.encode(tone)) }
            }

            override fun close() {
                closed++
            }
        }
    }

    companion object {
        const val LEVEL: Short = 1_000
    }
}
