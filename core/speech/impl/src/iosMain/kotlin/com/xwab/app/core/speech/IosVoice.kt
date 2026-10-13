@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.speech

import dev.zacsweers.metro.Inject
import kotlinx.cinterop.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.FileSystem
import okio.Path
import platform.AVFAudio.AVAudioBuffer
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate

/**
 * The system's speech synthesizer, written to buffers rather than played.
 *
 * `AVSpeechSynthesizer.writeUtterance` hands the spoken audio over in pieces and ends with an
 * empty one; this collects them and writes the speech as WAV, the format the Android voice writes too.
 */
@Inject
internal class IosVoice(private val fileSystem: FileSystem) : Voice {
    override suspend fun open(languageTag: String): VoiceSession {
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)
            ?: throw SpeechException("The device has no $languageTag voice.")
        return IosVoiceSession(voice, fileSystem)
    }
}

private class IosVoiceSession(
    private val voice: AVSpeechSynthesisVoice,
    private val fileSystem: FileSystem,
) : VoiceSession {
    /** Held for the session: a synthesizer that is released stops calling back. */
    private val synthesizer = AVSpeechSynthesizer()

    override val voiceId: String = voice.identifier

    override suspend fun speak(text: String, target: Path) {
        val pcm = synthesize(text) ?: throw SpeechException("The device's voice could not speak \"$text\".")
        fileSystem.write(target) { write(Wav.encode(pcm)) }
    }

    private suspend fun synthesize(text: String): Pcm? {
        val pieces = Channel<SpokenPiece>(Channel.UNLIMITED)
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = voice
        // A little slower than conversation: what it says is for picturing and following along at rest.
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * SPEECH_RATE
        withContext(Dispatchers.Main) {
            synthesizer.writeUtterance(utterance) { buffer -> pieces.trySend(spokenPiece(buffer)) }
        }

        val collected = mutableListOf<SpokenPiece.Audio>()
        while (true) {
            // The empty buffer that ends an utterance has not always arrived; a pause this long has.
            val piece = withTimeoutOrNull(IDLE_TIMEOUT_MS) { pieces.receive() } ?: break
            if (piece !is SpokenPiece.Audio) break
            collected += piece
        }
        return collected.takeIf { it.isNotEmpty() }?.let { audio ->
            Pcm(audio.first().sampleRate, audio.flatMap { it.samples.asList() }.toShortArray())
        }
    }

    /**
     * Nothing to release: the synthesizer only writes, it holds no audio session or engine, and it
     * goes with this session. An utterance cut short by cancellation finishes into a channel nobody reads.
     */
    override fun close() = Unit

    private companion object {
        const val SPEECH_RATE = 0.85f

        /** Pieces arrive milliseconds apart while something is spoken; this long a gap means it is over. */
        const val IDLE_TIMEOUT_MS = 2_000L
    }
}

/** One callback's worth of speech, copied out before the synthesizer reuses its buffer. */
private sealed interface SpokenPiece {
    class Audio(val sampleRate: Int, val samples: ShortArray) : SpokenPiece

    /** The empty buffer that ends an utterance, or one in a sample format this module does not read. */
    object End : SpokenPiece
}

private fun spokenPiece(buffer: AVAudioBuffer?): SpokenPiece {
    val pcm = buffer as? AVAudioPCMBuffer
    val frames = pcm?.frameLength?.toInt() ?: 0
    if (pcm == null || frames == 0) return SpokenPiece.End
    // Each accessor answers only for its own sample format and is null otherwise, so which one
    // answers says what the voice wrote.
    val int16 = pcm.int16ChannelData?.get(0)
    val float32 = pcm.floatChannelData?.get(0)
    val samples = when {
        int16 != null -> ShortArray(frames) { int16[it] }
        float32 != null -> ShortArray(frames) { (float32[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        else -> null
    }
    return samples?.let { SpokenPiece.Audio(pcm.format.sampleRate.toInt(), it) } ?: SpokenPiece.End
}
