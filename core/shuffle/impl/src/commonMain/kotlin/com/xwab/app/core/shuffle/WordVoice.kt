package com.xwab.app.core.shuffle

import okio.Path

/**
 * The device's own text-to-speech, opened for one language at a time.
 *
 * Each platform binds its own: Android's `TextToSpeech` and iOS's `AVSpeechSynthesizer`. Both are
 * already on the device in its own languages, so this module ships no recordings and sends nothing
 * anywhere to have a word spoken.
 */
internal fun interface WordVoice {
    /** @throws ShuffleRenderException when the device has no voice it can use for [languageTag]. */
    suspend fun open(languageTag: String): VoiceSession
}

/** One opened voice. Closing it releases the platform's engine. */
internal interface VoiceSession : AutoCloseable {
    /**
     * Names the voice actually speaking. A word spoken once is kept and reused, and this keeps a
     * word spoken by the voice the listener used to have from being reused after they change it.
     */
    val voiceId: String

    /**
     * Speaks [word] into a 16-bit PCM WAV file at [target].
     *
     * @throws ShuffleRenderException when the voice could not speak it.
     */
    suspend fun speak(word: String, target: Path)
}

/** Why a deck's track could not be made: no voice for its language, or no room to write it. */
internal class ShuffleRenderException(message: String, cause: Throwable? = null) : Exception(message, cause)
