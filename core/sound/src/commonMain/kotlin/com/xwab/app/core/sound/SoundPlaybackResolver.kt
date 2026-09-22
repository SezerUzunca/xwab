package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.delivery.port.DeliveryResult
import com.xwab.app.core.resolution.port.ItemResolution
import com.xwab.app.core.resolution.port.PlaybackItemResolver
import com.xwab.app.core.resolution.port.PlaybackPolicy
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sources.port.SOUND_NAMESPACE
import com.xwab.app.core.sources.port.SourcePort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.StringKey
import kotlinx.coroutines.flow.first

/**
 * What playing a sound means: metadata from this module's catalog, a URI from delivery.
 *
 * It lives here rather than in `:core:session` because this is the module that knows what a sound
 * is. The session holds one playback for whatever the app can play and looks a resolver up by kind;
 * it never names this class, this module, or the two it reaches through. That is what makes a
 * content type pluggable — and what makes this the only file that has to exist for a fourth one.
 *
 * [SOUND_PLAYBACK_KIND] is the map key rather than a literal, so the kind a screen asks for and the
 * kind that answers cannot drift apart.
 *
 * All three dependencies are `implementation`: `:core:delivery` and `:core:sources` are off limits
 * to features, and nothing this module publishes names a type from either, so they stop here rather
 * than travelling onto the compile classpath of every screen that reads a catalog.
 *
 * Delivery answers with a local file when the track is cached and with the HTTPS source when it is
 * not, starting the download in the background either way. That behaviour belongs to sounds and
 * stays here: a story streams and is not kept, so it must never be resolved through this path.
 */
@ContributesIntoMap(AppScope::class)
@StringKey(SOUND_PLAYBACK_KIND)
@Inject
internal class SoundPlaybackResolver(
    private val catalog: SoundPort,
    private val sources: SourcePort,
    private val content: DeliveryPort,
) : PlaybackItemResolver {

    override suspend fun resolve(value: String): ItemResolution {
        val trackId = TrackId(value)
        val track = catalog.observeTrack(trackId).first() ?: return ItemResolution.NotFound
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
                title = track.playbackTitle,
                // The name the lists show. It differs from the playback title often enough to
                // matter — the row a listener taps says "Rain on the Window" where the
                // notification says "Gentle Rain".
                displayName = track.name,
                artist = track.playbackArtist,
                policy = SOUND_POLICY,
            )
            is DeliveryResult.Unavailable -> ItemResolution.Unavailable(resolution.reason)
        }
    }
}

/** Sleep sounds loop until something stops them; that is the product, not the engine's default. */
private val SOUND_POLICY = PlaybackPolicy(defaultLooping = true)
