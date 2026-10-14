@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.nap

import com.xwab.app.core.nap.port.NAP_PLAYBACK_KIND
import com.xwab.app.core.nap.port.NapId
import com.xwab.app.core.nap.port.NapPort
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class NapTest {
    /** Nap ids and the kind are stored on devices, in the engine's source id; a rename strands them. */
    @Test
    fun theShippedNapsKeepTheirIdsAndKind() {
        assertEquals(listOf("nap-10", "nap-15", "nap-20"), naps.map { it.id.value })
        assertEquals(listOf(10, 15, 20), naps.map { it.minutes })
        assertEquals("nap", NAP_PLAYBACK_KIND)
    }

    /**
     * The wake-up starts exactly the nap's length after play: the settling line is held that long,
     * whatever the voice's pace. Then the wake-up, a few times over, and nothing that loops.
     */
    @Test
    fun theWakeUpStartsWhenTheNapIsOver() = runTest {
        val speech = RecordingSpeech()

        val resolved = assertIs<ItemResolution.Resolved>(resolver(speech).resolve("nap-20"))

        assertEquals("/tracks/nap/nap-20.wav", resolved.uri)
        assertEquals("20-Minute Nap", resolved.title)
        assertEquals("20-Minute Nap", resolved.displayName)
        assertNull(resolved.artist)
        assertFalse(resolved.policy.looping, "a nap ends with its wake-up")
        val request = speech.requests.single()
        assertEquals("nap", request.folder)
        assertEquals("nap-20", request.name)
        assertTrue(request.reuse, "the same words and length every time")
        assertNotNull(request.bed)
        val settle = assertIs<SpeechSegment.Spoken>(request.segments.first())
        assertEquals(20 * 60.0, settle.minSeconds)
        assertTrue("20 minutes" in settle.text)
        val spoken = request.segments.filterIsInstance<SpeechSegment.Spoken>()
        assertEquals(WAKE_ROUNDS, spoken.count { it.text == "It is time to wake up." })
        assertIs<SpeechSegment.Pause>(request.segments.last())
    }

    /** Only the opening line is held: the wake-up runs about a minute after it, not another nap. */
    @Test
    fun theWakeUpIsShort() {
        val afterSettle = napRequest(naps.first()).segments.drop(1)
        val held = afterSettle.sumOf { segment ->
            when (segment) {
                is SpeechSegment.Spoken -> segment.minSeconds
                is SpeechSegment.Pause -> segment.seconds
            }
        }
        assertTrue(held in 30.0..120.0, "the wake-up holds $held s")
    }

    @Test
    fun anUnknownNapIsNotFound() = runTest {
        assertEquals(ItemResolution.NotFound, resolver(RecordingSpeech()).resolve("nap-90"))
    }

    @Test
    fun aNapSpeechCannotVoiceIsUnavailable() = runTest {
        val resolution = resolver(RecordingSpeech(unavailable = "No en-US voice.")).resolve("nap-10")

        assertEquals(ItemResolution.Unavailable("No en-US voice."), resolution)
    }

    @Test
    fun theProductionGraphSharesItsCatalogAndResolvesEveryNap() = runTest {
        val speech = RecordingSpeech()
        val graph = createGraphFactory<NapGraph.Factory>().create(speech)

        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        assertEquals(naps, graph.catalog.observeNaps().first())
        naps.forEach { assertIs<ItemResolution.Resolved>(graph.resolver.resolve(it.id.value)) }
        assertEquals(naps.size, speech.requests.size)
    }

    @Test
    fun napsRefuseWhatCannotBePlayed() = runTest {
        assertFailsWith<IllegalArgumentException> { NapId(" ") }
        assertFailsWith<IllegalArgumentException> { naps.first().copy(minutes = 0) }
        assertFailsWith<IllegalArgumentException> { ManifestNapCatalogAdapter(naps + naps.first()) }
        assertEquals(null, ManifestNapCatalogAdapter(naps).observeNap(NapId("none")).first())
    }

    private fun resolver(speech: SpeechPort, catalog: NapPort = ManifestNapCatalogAdapter(naps)) =
        NapPlaybackResolver(catalog, speech)

    private class RecordingSpeech(private val unavailable: String? = null) : SpeechPort {
        val requests = mutableListOf<SpeechRequest>()

        override suspend fun render(request: SpeechRequest): SpeechResult {
            requests += request
            return unavailable?.let(SpeechResult::Unavailable)
                ?: SpeechResult.Rendered("/tracks/${request.folder}/${request.name}.wav")
        }
    }
}
