package com.xwab.app.core.session

import com.xwab.app.core.session.port.DEFAULT_LOOPING
import com.xwab.app.core.session.port.PlaybackKind
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.delivery.port.DeliveryResult
import kotlinx.coroutines.flow.first

/**
 * Sounds: metadata from the catalog, a URI from delivery.
 *
 * This is where the two sound modules the session depends on are actually used, and where a raw
 * item value becomes a `TrackId` again. Both dependencies are `implementation`, so neither type
 * appears in anything this module publishes.
 *
 * Delivery answers with a local file when the track is cached and with the HTTPS source when it is
 * not, starting the download in the background either way. That behaviour belongs to sounds and
 * stays here: a story streams and is not kept, so it must never be resolved through this path.
 */
internal class SoundPlaybackResolver(
    private val catalog: SoundPort,
    private val content: DeliveryPort,
) : PlaybackItemResolver {
    override val kind: PlaybackKind = PlaybackKind.SOUND

    override suspend fun resolve(value: String): ItemResolution {
        val trackId = TrackId(value)
        val music = catalog.observeMusic(trackId).first() ?: return ItemResolution.NotFound
        val source = catalog.sourceFor(trackId)
            ?: return ItemResolution.Unavailable("sound source is missing")

        val request = DeliveryRequest(
            key = CacheKey("sound", source.cacheFileName),
            httpsUrl = source.httpsUrl,
            acceptedContentTypes = setOf("audio/mpeg", "application/octet-stream"),
            retainedFileNames = catalog.cacheFileNames,
            headers = mapOf("User-Agent" to SOUND_USER_AGENT),
        )
        return when (val resolution = content.resolve(request)) {
            is DeliveryResult.Resolved -> ItemResolution.Resolved(
                uri = resolution.uri,
                title = music.playbackTitle,
                artist = music.playbackArtist,
                policy = SOUND_POLICY,
            )
            is DeliveryResult.Unavailable -> ItemResolution.Unavailable(resolution.reason)
        }
    }
}

/**
 * Identifies this client to the hosts the catalog points at.
 *
 * Every shipped source is served by Wikimedia, whose user-agent policy refuses a request that does
 * not say who is making it. That refusal arrives as a 4xx, which delivery classifies as a source
 * that will never work: no retry, and the file is not cached. Playback would keep streaming over
 * HTTPS through the platform player, so the only visible effect would be a cache that quietly never
 * fills — which is why this header is carried deliberately rather than left to a default.
 */
private const val SOUND_USER_AGENT = "SleepSounds/1.0 (audio cache)"

/** Sleep sounds loop until something stops them; that is the product, not the engine's default. */
private val SOUND_POLICY = PlaybackPolicy(defaultLooping = DEFAULT_LOOPING)
