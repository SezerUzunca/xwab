@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.createGraphFactory
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assume.assumeTrue

/**
 * The Android production graph, with the Context and the text-to-speech engine only a device has.
 *
 * The last two tests need a text-to-speech engine, as the app does. Phones ship one, and so do the
 * emulator images with Google APIs; CI's plain AOSP image may not, and there the two are reported
 * as skipped rather than passed. An engine without an English voice fails them, saying so.
 */
class AndroidShuffleGraphTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cache: File get() = context.cacheDir.resolve("shuffle")

    private fun assumeAnEngine() = assumeTrue(
        "This device has no text-to-speech engine.",
        context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            .isNotEmpty(),
    )

    @AfterTest
    fun removeWhatTheTestsWrote() {
        cache.deleteRecursively()
    }

    /** The app graph may build this graph on the main thread; building it starts no engine and reads no directory. */
    @Test
    fun theModuleGraphSharesItsCatalogAndListsTheShippedDecks() = runBlocking {
        val graph = createGraphFactory<AndroidShuffleGraph.Factory>().create(context)

        assertSame(graph.catalog, graph.catalog)
        assertEquals(shuffleDecks, graph.catalog.observeDecks().first())
    }

    /** The device's own engine writes a word as WAV this module reads back. */
    @Test
    fun theDevicesVoiceSpeaksAWordThisModuleCanRead() = runBlocking {
        assumeAnEngine()
        val target = cache.resolve("kite.wav").also { it.parentFile?.mkdirs() }

        AndroidWordVoice(context).open("en-US").use { voice -> voice.speak("kite", target.absolutePath.toPath()) }

        val spoken = Wav.decode(target.readBytes())
        val shortest = spoken.sampleRate / TENTHS_PER_SECOND
        assertTrue(spoken.samples.size > shortest, "a tenth of a second at least: ${spoken.samples.size}")
        assertTrue(spoken.samples.any { it != 0.toShort() }, "the word is not silence")
    }

    private companion object {
        const val TENTHS_PER_SECOND = 10
    }

    /** Nothing replaced: the graph's resolver speaks the shipped deck into a track on disk. */
    @Test
    fun theUnmodifiedGraphRendersTheShippedDeck() = runBlocking {
        assumeAnEngine()
        val graph = createGraphFactory<AndroidShuffleGraph.Factory>().create(context)
        val deck = shuffleDecks.first()

        val resolved = assertIs<ItemResolution.Resolved>(graph.resolver.resolve(deck.id.value))

        val track = Wav.decode(File(resolved.uri).readBytes())
        val words = shuffleWordLists.single { it.deckId == deck.id.value }.wordsPerTrack
        assertEquals(words * deck.wordIntervalSeconds * track.sampleRate, track.samples.size)
        assertTrue(resolved.policy.looping)
    }
}
