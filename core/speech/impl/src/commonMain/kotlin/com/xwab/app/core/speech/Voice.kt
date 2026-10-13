package com.xwab.app.core.speech

import okio.Path

/**
 * The device's own text-to-speech, opened for one language at a time.
 *
 * Each platform binds its own: Android's `TextToSpeech` and iOS's `AVSpeechSynthesizer`. Both are
 * already on the device in its own languages, so this module ships no recordings and sends nothing
 * anywhere to have a word spoken.
 */
internal fun interface Voice {
    /** @throws SpeechException when the device has no voice it can use for [languageTag]. */
    suspend fun open(languageTag: String): VoiceSession
}

/** One opened voice. Closing it releases the platform's engine. */
internal interface VoiceSession : AutoCloseable {
    /**
     * Names the voice actually speaking. What it speaks once is kept and reused, and this keeps
     * speech made by the voice the listener used to have from being reused after they change it.
     */
    val voiceId: String

    /**
     * Speaks [text] into a 16-bit PCM WAV file at [target].
     *
     * @throws SpeechException when the voice could not speak it.
     */
    suspend fun speak(text: String, target: Path)
}

/** Why a track could not be made: no voice for its language, or no room to write it. */
internal class SpeechException(message: String, cause: Throwable? = null) : Exception(message, cause)
