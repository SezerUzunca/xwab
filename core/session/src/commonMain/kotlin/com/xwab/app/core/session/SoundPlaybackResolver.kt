package com.xwab.app.core.session

import com.xwab.app.core.session.port.PlaybackKind
import com.xwab.app.core.sources.port.SOUND_NAMESPACE
import com.xwab.app.core.sources.port.SourcePort
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
 * This is where metadata, physical-source and delivery capabilities meet, and where a raw item
 * value becomes a `TrackId` again. All three dependencies are `implementation`, so none of their
 * types appears in anything this module publishes.
 *
 * Delivery answers with a local file when the track is cached and with the HTTPS source when it is
 * not, starting the download in the background either way. That behaviour belongs to sounds and
 * stays here: a story streams and is not kept, so it must never be resolved through this path.
 */
internal class SoundPlaybackResolver(
    private val catalog: SoundPort,
    private val sources: SourcePort,
    private val content: DeliveryPort,
) : PlaybackItemResolver {
    override val kind: PlaybackKind = PlaybackKind.SOUND

    override suspend fun resolve(value: String): ItemResolution {
        val trackId = TrackId(value)
        val music = catalog.observeMusic(trackId).first() ?: return ItemResolution.NotFound
        val source = sources.sourceFor(SOUND_NAMESPACE, value)
            ?: return ItemResolution.Unavailable("sound source is missing")
        val cacheFileName = source.cacheFileName
            ?: return ItemResolution.Unavailable("sound cache filename is missing")

        val request = DeliveryRequest(
            key = CacheKey(SOUND_NAMESPACE, cacheFileName),
            httpsUrl = source.httpsUrl,
            acceptedContentTypes = setOf("audio/mpeg", "application/octet-stream"),
            retainedFileNames = sources.cacheFileNames(SOUND_NAMESPACE),
            headers = source.headers,
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

/** Sleep sounds loop until something stops them; that is the product, not the engine's default. */
private val SOUND_POLICY = PlaybackPolicy(defaultLooping = true)
