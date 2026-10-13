@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.rehearsal

import com.xwab.app.core.notes.port.NoteSaveResult
import com.xwab.app.core.notes.port.NotesPort
import com.xwab.app.core.rehearsal.port.REHEARSAL_PLAYBACK_KIND
import com.xwab.app.core.rehearsal.port.REHEARSAL_SCRIPT_MAX_LENGTH
import com.xwab.app.core.rehearsal.port.RehearsalSaveResult
import com.xwab.app.core.rehearsal.port.RehearsalScriptId
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class RehearsalTest {
    /** The script's key and the kind are names on devices: a rename strands what was written or played. */
    @Test
    fun theStoredNamesStayPut() {
        assertEquals("rehearsal", SCRIPT_NOTE_KEY)
        assertEquals("rehearsal", REHEARSAL_PLAYBACK_KIND)
    }

    @Test
    fun aSavedScriptIsTrimmedAndNamedByItsText() = runTest {
        val notes = FakeNotes()
        val port = NotesRehearsalAdapter(notes)
        assertNull(port.observeScript().first(), "nothing written yet")

        assertEquals(RehearsalSaveResult.Saved, port.save("  The wave sets me down on the sand.\n"))

        assertEquals("The wave sets me down on the sand.", notes.text.value)
        val script = assertNotNull(port.observeScript().first())
        assertEquals("The wave sets me down on the sand.", script.text)
        assertEquals(scriptIdOf("The wave sets me down on the sand."), script.id)
    }

    @Test
    fun aBlankScriptIsRemovedAndARefusedWriteIsReported() = runTest {
        val notes = FakeNotes(stored = "A calm room.")
        val port = NotesRehearsalAdapter(notes)

        assertEquals(RehearsalSaveResult.Saved, port.save("   "))
        assertNull(port.observeScript().first())

        assertEquals(RehearsalSaveResult.Unavailable, NotesRehearsalAdapter(FakeNotes(refuse = true)).save("Hi."))
        assertFailsWith<IllegalArgumentException> { port.save("a".repeat(REHEARSAL_SCRIPT_MAX_LENGTH + 1)) }
    }

    /**
     * The id is stored in the engine's source id, so it must be the same on every run and platform;
     * and an edit must change it, or the session would resume the old wording's track.
     */
    @Test
    fun aWordingsIdIsStableAndChangesWithItsText() {
        assertEquals(RehearsalScriptId("cbf29ce484222325"), scriptIdOf(""))
        assertEquals(RehearsalScriptId("af63dc4c8601ec8c"), scriptIdOf("a"))
        assertEquals(scriptIdOf("I am safe at home."), scriptIdOf("I am safe at home."))
        assertNotEquals(scriptIdOf("I am safe at home."), scriptIdOf("I am safe at home!"))
        assertEquals(16, scriptIdOf("ğüşöç — 夢").value.length)
    }

    @Test
    fun theScriptIsReadASentenceAtATime() {
        assertEquals(
            listOf("The door opens.", "Light comes in!", "Who is there?", "It is my friend", "We laugh."),
            sentencesOf("The door opens. Light comes in! Who is there?\nIt is my friend\r\n\n  We laugh.  "),
        )
        val unbroken = "It is 3.5 metres away, Dr.Smith said"
        assertEquals(listOf(unbroken), sentencesOf(unbroken), "a stop inside a word ends nothing")
        assertEquals(emptyList(), sentencesOf(" \n "))
    }

    /** The framing, then each sentence with its pause, over a bed; made once per wording. */
    @Test
    fun theStoredWordingBecomesASpokenTrackThatPlaysOnce() = runTest {
        val notes = FakeNotes(stored = "The wave sets me down. I walk home.")
        val speech = RecordingSpeech()
        val resolver = RehearsalPlaybackResolver(NotesRehearsalAdapter(notes), speech)

        val resolved = assertIs<ItemResolution.Resolved>(resolver.resolve(scriptIdOf(notes.text.value).value))

        assertEquals("/tracks/rehearsal/own-dream.wav", resolved.uri)
        assertEquals(REHEARSAL_TITLE, resolved.title)
        assertEquals(REHEARSAL_TITLE, resolved.displayName)
        assertNull(resolved.artist)
        assertFalse(resolved.policy.looping, "a rehearsal is gone through once")
        val request = speech.requests.single()
        assertEquals("rehearsal", request.folder)
        assertTrue(request.reuse, "the same words make the same track")
        assertNotNull(request.bed)
        assertEquals(SpeechSegment.Pause(3.0), request.segments.first())
        val spoken = request.segments.filterIsInstance<SpeechSegment.Spoken>().map { it.text }
        val own = spoken.indexOf("The wave sets me down.")
        assertEquals("Here is your new dream.", spoken[own - 1])
        assertEquals("I walk home.", spoken[own + 1])
        assertEquals(
            SpeechSegment.Pause(SENTENCE_PAUSE_SECONDS),
            request.segments[request.segments.indexOf(SpeechSegment.Spoken("I walk home.")) + 1],
        )
        assertEquals("Take a slow breath, and open your eyes when you are ready.", spoken.last())
    }

    /** An earlier wording the engine still holds is gone, as a removed guide is. */
    @Test
    fun onlyTheStoredWordingResolves() = runTest {
        val notes = FakeNotes(stored = "I walk home.")
        val speech = RecordingSpeech()
        val resolver = RehearsalPlaybackResolver(NotesRehearsalAdapter(notes), speech)

        assertEquals(ItemResolution.NotFound, resolver.resolve(scriptIdOf("An older wording.").value))
        notes.text.value = ""
        assertEquals(ItemResolution.NotFound, resolver.resolve(scriptIdOf("I walk home.").value))
        assertTrue(speech.requests.isEmpty(), "nothing is spoken for a wording that is gone")
    }

    @Test
    fun aScriptSpeechCannotVoiceIsUnavailable() = runTest {
        val notes = FakeNotes(stored = "I walk home.")
        val resolver = RehearsalPlaybackResolver(NotesRehearsalAdapter(notes), RecordingSpeech("No en-US voice."))

        assertEquals(ItemResolution.Unavailable("No en-US voice."), resolver.resolve(scriptIdOf("I walk home.").value))
    }

    @Test
    fun theProductionGraphSharesItsPortWithTheResolver() = runTest {
        val notes = FakeNotes()
        val speech = RecordingSpeech()
        val graph = createGraphFactory<RehearsalGraph.Factory>().create(notes, speech)

        assertSame(graph.rehearsal, graph.rehearsal)
        assertSame(graph.resolver, graph.resolver)
        graph.rehearsal.save("I walk home.")
        val id = assertNotNull(graph.rehearsal.observeScript().first()).id
        assertIs<ItemResolution.Resolved>(graph.resolver.resolve(id.value))
        assertEquals(1, speech.requests.size)
    }

    private class FakeNotes(stored: String = "", private val refuse: Boolean = false) : NotesPort {
        val text = MutableStateFlow(stored)

        override fun observe(key: String): Flow<String> {
            assertEquals(SCRIPT_NOTE_KEY, key)
            return text
        }

        override suspend fun save(key: String, text: String): NoteSaveResult {
            assertEquals(SCRIPT_NOTE_KEY, key)
            if (refuse) return NoteSaveResult.Unavailable
            this.text.value = text
            return NoteSaveResult.Saved
        }
    }

    private class RecordingSpeech(private val unavailable: String? = null) : SpeechPort {
        val requests = mutableListOf<SpeechRequest>()

        override suspend fun render(request: SpeechRequest): SpeechResult {
            requests += request
            return unavailable?.let(SpeechResult::Unavailable)
                ?: SpeechResult.Rendered("/tracks/${request.folder}/${request.name}.wav")
        }
    }
}
