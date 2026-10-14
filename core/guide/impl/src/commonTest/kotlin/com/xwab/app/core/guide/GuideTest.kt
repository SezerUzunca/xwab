@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.guide

import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuideUse
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.createGraphFactory
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GuideTest {
    @Test
    fun everyGuideHasItsScriptAndEveryScriptItsGuide() {
        assertEquals(guides.map { it.id.value }.sorted(), guideScripts.map { it.guideId }.sorted())
    }

    /** Guide ids are stored on devices, in the engine's source id; a rename strands them. */
    @Test
    fun theShippedGuidesKeepTheirIds() {
        assertEquals(
            listOf("muscle-relaxation", "quiet-beach", "forest-path", "rain-cabin", "out-of-bed"),
            guides.map { it.id.value },
        )
    }

    /**
     * "About eight minutes" has to be roughly true. The voice sets the pace, so this estimates
     * speech at three words a second, as the emulator's Google voice spoke the muscle-relaxation
     * guide (7.6 minutes), and allows a third either way.
     */
    @Test
    fun everyGuideRunsAboutAsLongAsItSays() {
        guides.forEach { guide ->
            val script = guideScripts.single { it.guideId == guide.id.value }
            val words = script.lines.sumOf { line -> line.text.split(' ').size }
            val seconds = 3.0 + CLOSING_SECONDS + script.lines.sumOf { it.pauseSeconds } + words / WORDS_PER_SECOND
            val declared = guide.minutes * 60.0
            assertTrue(abs(seconds - declared) / declared < 1.0 / 3, "${guide.id} runs about ${seconds / 60} min")
        }
    }

    /**
     * The out-of-bed guide is heard sitting up after lying awake. Stimulus control sends the
     * listener back to bed only once they are sleepy, so it must not settle them down where they sit.
     */
    @Test
    fun theOutOfBedGuideNeverSettlesTheListenerDownWhereTheySit() {
        assertEquals(listOf("out-of-bed"), guides.filter { it.use == GuideUse.OutOfBed }.map { it.id.value })
        val spoken = guideScripts.single { it.guideId == "out-of-bed" }.lines.joinToString(" ") { it.text.lowercase() }
        listOf("close your eyes", "eyes close", "lie down", "drift").forEach { phrase ->
            assertFalse(phrase in spoken, "the out-of-bed guide says \"$phrase\"")
        }
    }

    @Test
    fun aGuideBecomesASpokenTrackThatPlaysOnce() = runTest {
        val speech = RecordingSpeech()

        val resolved = assertIs<ItemResolution.Resolved>(resolver(speech).resolve("muscle-relaxation"))

        assertEquals("/tracks/guide/muscle-relaxation.wav", resolved.uri)
        assertEquals("Muscle Relaxation", resolved.title)
        assertEquals("Muscle Relaxation", resolved.displayName)
        assertNull(resolved.artist)
        assertFalse(resolved.policy.looping, "a guide ends, as a story does")
    }

    /** The script in order, each line followed by its pause, between the bed's opening and its close. */
    @Test
    fun theTrackIsTheScriptOverABedAndIsMadeOnce() = runTest {
        val speech = RecordingSpeech()

        resolver(speech).resolve("quiet-beach")

        val request = speech.requests.single()
        val script = guideScripts.single { it.guideId == "quiet-beach" }
        assertEquals("guide", request.folder)
        assertEquals("quiet-beach", request.name)
        assertTrue(request.reuse, "the same words every time: made once")
        assertNotNull(request.bed)
        assertEquals(SpeechSegment.Pause(3.0), request.segments.first())
        assertEquals(SpeechSegment.Pause(CLOSING_SECONDS), request.segments.last())
        assertEquals(
            script.lines.flatMap { listOf(SpeechSegment.Spoken(it.text), SpeechSegment.Pause(it.pauseSeconds)) },
            request.segments.drop(1).dropLast(1),
        )
    }

    @Test
    fun anUnknownGuideIsNotFound() = runTest {
        assertEquals(ItemResolution.NotFound, resolver(RecordingSpeech()).resolve("no-such-guide"))
    }

    @Test
    fun aGuideSpeechCannotVoiceIsUnavailable() = runTest {
        val resolution = resolver(RecordingSpeech(unavailable = "No en-US voice.")).resolve("forest-path")

        assertEquals(ItemResolution.Unavailable("No en-US voice."), resolution)
    }

    @Test
    fun aGuideWithoutAScriptIsUnavailable() = runTest {
        assertIs<ItemResolution.Unavailable>(resolver(RecordingSpeech(), scripts = emptyList()).resolve("rain-cabin"))
    }

    @Test
    fun theProductionGraphSharesItsCatalogAndResolvesEveryGuide() = runTest {
        val speech = RecordingSpeech()
        val graph = createGraphFactory<GuideGraph.Factory>().create(speech)

        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        assertEquals(guides, graph.catalog.observeGuides().first())
        guides.forEach { assertIs<ItemResolution.Resolved>(graph.resolver.resolve(it.id.value)) }
        assertEquals(guides.size, speech.requests.size)
    }

    @Test
    fun guidesAndScriptsRefuseWhatCannotBePlayed() = runTest {
        assertFailsWith<IllegalArgumentException> { GuideId(" ") }
        assertFailsWith<IllegalArgumentException> { guides.first().copy(minutes = 0) }
        assertFailsWith<IllegalArgumentException> { line("Rest.", 0) }
        assertFailsWith<IllegalArgumentException> { GuideScript("rest", emptyList()) }
        assertFailsWith<IllegalArgumentException> { ManifestGuideCatalogAdapter(guides + guides.first()) }
        assertFailsWith<IllegalArgumentException> { resolver(RecordingSpeech(), guideScripts + guideScripts.first()) }
        assertEquals(null, ManifestGuideCatalogAdapter(guides).observeGuide(GuideId("none")).first())
    }

    private fun resolver(speech: SpeechPort, scripts: List<GuideScript> = guideScripts) =
        GuidePlaybackResolver(ManifestGuideCatalogAdapter(guides), scripts, speech)

    private class RecordingSpeech(private val unavailable: String? = null) : SpeechPort {
        val requests = mutableListOf<SpeechRequest>()

        override suspend fun render(request: SpeechRequest): SpeechResult {
            requests += request
            return unavailable?.let(SpeechResult::Unavailable)
                ?: SpeechResult.Rendered("/tracks/${request.folder}/${request.name}.wav")
        }
    }

    private companion object {
        const val WORDS_PER_SECOND = 3.0
    }
}
