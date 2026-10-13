package com.xwab.app.core.shuffle

import com.xwab.app.core.shuffle.port.ShuffleDeck
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
 * Speaks a fresh selection of a deck's words into one looping track on the device.
 *
 * Every render draws new words in a new order, so no two nights are the same sequence; the session
 * resolves again only when it is not already holding the track, so pausing and resuming keeps the
 * one that was playing. Words are spoken once per voice and kept, and a deck's earlier tracks are
 * deleted when a new one is written, so the cache holds one track a deck and the words already
 * spoken — never a growing pile.
 *
 * Renders run one at a time: two at once would race to delete each other's track.
 */
@SingleIn(ShuffleScope::class)
@Inject
internal class ShuffleTrackRenderer(
    private val voice: WordVoice,
    private val location: ShuffleCacheLocation,
    private val fileSystem: FileSystem,
    private val fileDispatcher: CoroutineDispatcher,
    private val random: Random,
) {
    private val renders = Mutex()

    /**
     * @return the absolute path of the new track, which the platform players open as a local file.
     * @throws ShuffleRenderException when the device has no voice for the deck or the track could
     *   not be written.
     */
    suspend fun render(deck: ShuffleDeck, wordList: ShuffleWordList): Path = renders.withLock {
        withContext(fileDispatcher) {
            try {
                renderTrack(deck, wordList)
            } catch (failure: IOException) {
                throw ShuffleRenderException("Could not write the ${deck.id} track.", failure)
            }
        }
    }

    private suspend fun renderTrack(deck: ShuffleDeck, wordList: ShuffleWordList): Path {
        val words = wordList.words.shuffled(random).take(wordList.wordsPerTrack)
        val spoken = voice.open(deck.languageTag).use { session -> words.map { spokenWord(session, it) } }

        val tracks = location.root() / TRACKS_DIRECTORY
        fileSystem.createDirectories(tracks)
        // A new name every time. A player that is handed the address it already has may take it for
        // the same source and keep the old audio, which this renderer has just deleted.
        val track = tracks / "${deck.id.value}-${random.nextLong().toULong().toString(RADIX)}.wav"
        val partial = tracks / "${track.name}$PARTIAL"
        fileSystem.write(partial) { writeShuffleTrack(this, spoken, deck.wordIntervalSeconds) }
        // The engine holds none of these: had it held this deck's track, the session would have
        // resumed it instead of asking for a new one.
        fileSystem.list(tracks)
            .filter { it.name.startsWith("${deck.id.value}-") && it != partial }
            .forEach(fileSystem::delete)
        fileSystem.atomicMove(partial, track)
        return track
    }

    /** The word as [session]'s voice speaks it, spoken now unless that voice has spoken it before. */
    private suspend fun spokenWord(session: VoiceSession, word: String): Pcm {
        val directory = location.root() / WORDS_DIRECTORY / fileNameOf(session.voiceId)
        val file = directory / "${fileNameOf(word)}.wav"
        if (!fileSystem.exists(file)) {
            fileSystem.createDirectories(directory)
            val partial = directory / "${file.name}$PARTIAL"
            session.speak(word, partial)
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

    private companion object {
        const val WORDS_DIRECTORY = "words"
        const val TRACKS_DIRECTORY = "tracks"
        const val PARTIAL = ".part"
        const val RADIX = 36

        /** Lower-case letters, digits and dashes: a voice id or a word, safe as a file name anywhere. */
        fun fileNameOf(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }
}
