package com.xwab.app.core.speech

import co.touchlab.kermit.Logger
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path

/**
 * Speaks a request's text with the device's voice and lays it out as one track on disk.
 *
 * Each piece of text is spoken once per voice and kept, so a later track that says it again only
 * reads it back; a different voice speaks it anew. A track replaces its predecessor of the same
 * name, so the cache holds one track a name and the speech already made — never a growing pile.
 * A reusable request whose track this voice has already made is answered with that file at once.
 *
 * Renders run one at a time: two at once would race to delete each other's track.
 */
@SingleIn(SpeechScope::class)
@Inject
internal class SpeechRendererAdapter(
    private val voice: Voice,
    private val location: SpeechCacheLocation,
    private val fileSystem: FileSystem,
    private val fileDispatcher: CoroutineDispatcher,
    private val random: Random,
    logger: Logger,
) : SpeechPort {
    private val logger = logger.withTag("SpeechRendererAdapter")
    private val renders = Mutex()

    override suspend fun render(request: SpeechRequest): SpeechResult = renders.withLock {
        withContext(fileDispatcher) {
            try {
                SpeechResult.Rendered(renderTrack(request).toString())
            } catch (failure: SpeechException) {
                unavailable(request, failure)
            } catch (failure: IOException) {
                unavailable(request, failure)
            }
        }
    }

    private suspend fun renderTrack(request: SpeechRequest): Path = voice.open(request.languageTag).use { session ->
        val tracks = location.root() / TRACKS_DIRECTORY / request.folder
        // A reusable track is named after what it holds, so the same request finds it again. Any
        // other gets a new name: a player handed the address it already has may take it for the
        // same source and keep the old audio, which this renderer is about to delete.
        val version = if (request.reuse) fingerprintOf(session.voiceId, request) else freshName()
        val track = tracks / "${request.name}$SEPARATOR$version.wav"
        if (request.reuse && fileSystem.exists(track)) return track

        val segments = request.segments.map { segment ->
            when (segment) {
                is SpeechSegment.Spoken -> TrackSegment.Speech(spoken(session, segment.text), segment.minSeconds)
                is SpeechSegment.Pause -> TrackSegment.Pause(segment.seconds)
            }
        }
        fileSystem.createDirectories(tracks)
        val partial = tracks / "${track.name}$PARTIAL"
        fileSystem.write(partial) { writeSpeechTrack(this, segments, request.bed, random) }
        // The engine holds none of these: had it held this name's track, the session would have
        // resumed it rather than asked for a track again.
        fileSystem.list(tracks)
            .filter { it.name.startsWith("${request.name}$SEPARATOR") && it != partial }
            .forEach(fileSystem::delete)
        fileSystem.atomicMove(partial, track)
        track
    }

    /** [text] as [session]'s voice speaks it, spoken now unless that voice has spoken it before. */
    private suspend fun spoken(session: VoiceSession, text: String): Pcm {
        val directory = location.root() / UTTERANCES_DIRECTORY / fileNameOf(session.voiceId)
        val file = directory / "${fingerprintOf(text)}.wav"
        if (!fileSystem.exists(file)) {
            fileSystem.createDirectories(directory)
            val partial = directory / "${file.name}$PARTIAL"
            session.speak(text, partial)
            fileSystem.atomicMove(partial, file)
        }
        val bytes = fileSystem.read(file) { readByteArray() }
        return try {
            Wav.decode(bytes)
        } catch (unreadable: IOException) {
            // Spoken again on the next render rather than failing every one after this.
            fileSystem.delete(file)
            throw unreadable
        }
    }

    private fun unavailable(request: SpeechRequest, failure: Exception): SpeechResult {
        logger.w(failure) { "Could not render ${request.folder}/${request.name}." }
        return SpeechResult.Unavailable(failure.message ?: failure::class.simpleName.orEmpty())
    }

    private fun freshName(): String = random.nextLong().toULong().toString(RADIX)

    private companion object {
        const val UTTERANCES_DIRECTORY = "utterances"
        const val TRACKS_DIRECTORY = "tracks"
        const val PARTIAL = ".part"

        /** Not allowed in a track name, so one name's tracks are never taken for another's. */
        const val SEPARATOR = "."
        const val RADIX = 36

        /**
         * Changed whenever the way a track is laid out changes, so tracks made the old way are not
         * reused.
         */
        const val TRACK_FORMAT = 1

        /** Lower-case letters, digits and dashes: a voice id, safe as a file name anywhere. */
        fun fileNameOf(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

        fun fingerprintOf(voiceId: String, request: SpeechRequest): String =
            fingerprintOf("$TRACK_FORMAT|$voiceId|${request.languageTag}|${request.segments}|${request.bed}")

        /** FNV-1a over the text's UTF-8 bytes: stable on every platform and in every process. */
        fun fingerprintOf(text: String): String {
            var hash = FNV_OFFSET
            text.encodeToByteArray().forEach { byte ->
                hash = (hash xor (byte.toULong() and BYTE_MASK)) * FNV_PRIME
            }
            return hash.toString(HEX).padStart(HASH_DIGITS, '0')
        }

        const val FNV_OFFSET = 0xcbf29ce484222325UL
        const val FNV_PRIME = 0x100000001b3UL
        const val BYTE_MASK = 0xFFUL
        const val HEX = 16
        const val HASH_DIGITS = 16
    }
}
