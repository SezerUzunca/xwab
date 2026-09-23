package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.delivery.port.DeliveryResult
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SoundPlaybackResolverTest {
    private val track = Track(
        id = TrackId("heavy-rain"),
        name = "Rain on the Window",
        categoryId = CategoryId("rain"),
        durationSeconds = 60,
        playbackTitle = "Gentle Rain",
        playbackArtist = "Field Recordings",
    )
    private val sources = SoundSources(
        listOf(
            soundSource("heavy-rain", "https://example.test/heavy-rain.mp3", version = 2),
            soundSource("calm-waves", "https://example.test/calm-waves.mp3"),
        ),
    )

    @Test
    fun anUnknownTrackDoesNotRequestDelivery() = runBlocking {
        val delivery = RecordingDelivery()
        val resolver = resolver(delivery)

        assertEquals(ItemResolution.NotFound, resolver.resolve("no-such-track"))
        assertTrue(delivery.requests.isEmpty())
    }

    @Test
    fun aMissingPhysicalSourceIsUnavailableWithoutRequestingDelivery() = runBlocking {
        val delivery = RecordingDelivery()
        val resolver = resolver(delivery, SoundSources(emptyList()))

        assertEquals(ItemResolution.Unavailable("sound source is missing"), resolver.resolve(track.id.value))
        assertTrue(delivery.requests.isEmpty())
    }

    @Test
    fun resolvedSoundUsesDeliveryUriAndPreservesMetadataHeadersAndCachePolicy() = runBlocking {
        val delivery = RecordingDelivery(DeliveryResult.Resolved("file:///cache/heavy-rain-v2.mp3"))

        val result = assertIs<ItemResolution.Resolved>(resolver(delivery).resolve(track.id.value))
        val request = delivery.requests.single()

        assertEquals("file:///cache/heavy-rain-v2.mp3", result.uri)
        assertEquals(track.playbackTitle, result.title)
        assertEquals(track.name, result.displayName)
        assertEquals(track.playbackArtist, result.artist)
        assertTrue(result.policy.defaultLooping)
        assertEquals(CacheKey("sound", "heavy-rain-v2.mp3"), request.key)
        assertEquals("https://example.test/heavy-rain.mp3", request.httpsUrl)
        assertEquals(setOf("audio/mpeg", "application/octet-stream"), request.acceptedContentTypes)
        assertEquals(setOf("heavy-rain-v2.mp3", "calm-waves-v1.mp3"), request.retainedFileNames)
        assertEquals(
            mapOf("User-Agent" to "SleepSounds/1.0 (https://github.com/SezerUzunca/xwab)"),
            request.headers,
        )
    }

    @Test
    fun deliveryFailureIsReturnedWithoutLosingItsReason() = runBlocking {
        val delivery = RecordingDelivery(DeliveryResult.Unavailable("connection unavailable"))

        assertEquals(
            ItemResolution.Unavailable("connection unavailable"),
            resolver(delivery).resolve(track.id.value),
        )
        assertEquals(1, delivery.requests.size)
    }

    private fun resolver(delivery: DeliveryPort, sources: SoundSources = this.sources) =
        SoundPlaybackResolver(
            catalog = ManifestSoundCatalogAdapter(listOf(track)),
            content = delivery,
            sources = sources,
        )

    private class RecordingDelivery(
        private val result: DeliveryResult = DeliveryResult.Resolved("https://example.test/audio.mp3"),
    ) : DeliveryPort {
        override suspend fun retainOnly(namespaces: Set<String>) = Unit

        val requests = mutableListOf<DeliveryRequest>()

        override suspend fun resolve(request: DeliveryRequest): DeliveryResult {
            requests += request
            return result
        }
    }
}
