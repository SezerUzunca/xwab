package com.xwab.app.core.shuffle

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dev.zacsweers.metro.Inject
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import okio.Path

/**
 * The device's text-to-speech engine, which writes each word straight to a WAV file.
 *
 * The engine is a separate app reached through a service, so starting it and every word it speaks
 * are answered on callbacks; each wait has a deadline, so an engine that never answers fails the
 * render instead of leaving the deck preparing forever.
 */
@Inject
internal class AndroidWordVoice(private val context: Context) : WordVoice {
    override suspend fun open(languageTag: String): VoiceSession {
        val started = CompletableDeferred<Int>()
        val engine = TextToSpeech(context) { status -> started.complete(status) }
        var opened = false
        try {
            val status = withTimeoutOrNull(ENGINE_START_TIMEOUT_MS) { started.await() }
            if (status != TextToSpeech.SUCCESS) {
                throw ShuffleRenderException("The device's text-to-speech engine did not start.")
            }
            val language = engine.setLanguage(Locale.forLanguageTag(languageTag))
            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw ShuffleRenderException("The device has no $languageTag voice.")
            }
            // A little slower than conversation: the words are for picturing, not for following.
            engine.setSpeechRate(SPEECH_RATE)
            opened = true
            return AndroidVoiceSession(engine)
        } finally {
            if (!opened) engine.shutdown()
        }
    }

    private companion object {
        const val ENGINE_START_TIMEOUT_MS = 10_000L
        const val SPEECH_RATE = 0.85f
    }
}

private class AndroidVoiceSession(private val engine: TextToSpeech) : VoiceSession {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val utterances = AtomicInteger()

    init {
        engine.setOnUtteranceProgressListener(Progress())
    }

    override val voiceId: String = listOfNotNull(engine.defaultEngine, engine.voice?.name).joinToString("-")

    override suspend fun speak(word: String, target: Path) {
        val utterance = "word-${utterances.incrementAndGet()}"
        val done = CompletableDeferred<Boolean>()
        pending[utterance] = done
        try {
            val queued = engine.synthesizeToFile(word, Bundle(), File(target.toString()), utterance)
            val spoken = queued == TextToSpeech.SUCCESS && withTimeoutOrNull(WORD_TIMEOUT_MS) { done.await() } == true
            if (!spoken) throw ShuffleRenderException("The device's voice could not speak \"$word\".")
        } finally {
            pending.remove(utterance)
        }
    }

    override fun close() {
        engine.shutdown()
    }

    private inner class Progress : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            utteranceId?.let(pending::get)?.complete(true)
        }

        @Deprecated("Superseded by onError(String, Int), which the engine calls on API 21 and later.")
        override fun onError(utteranceId: String?) {
            utteranceId?.let(pending::get)?.complete(false)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            utteranceId?.let(pending::get)?.complete(false)
        }
    }

    private companion object {
        /** One word takes a fraction of a second; this is for an engine that has stopped answering. */
        const val WORD_TIMEOUT_MS = 10_000L
    }
}
