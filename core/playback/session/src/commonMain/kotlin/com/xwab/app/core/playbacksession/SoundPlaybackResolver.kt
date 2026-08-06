package com.xwab.app.core.playbacksession

import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.audiodelivery.resolution.AudioSourceResolution
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
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
    private val catalog: MusicCatalogRepository,
    private val content: AudioContentResolver,
) : PlaybackItemResolver {
    override val kind: PlaybackKind = PlaybackKind.SOUND

    override suspend fun resolve(value: String): ItemResolution {
        val trackId = TrackId(value)
        val music = catalog.observeMusic(trackId).first() ?: return ItemResolution.NotFound

        return when (val resolution = content.resolve(trackId)) {
            is AudioSourceResolution.Resolved -> ItemResolution.Resolved(
                uri = resolution.uri,
                title = music.playbackTitle,
                artist = music.playbackArtist,
                policy = SOUND_POLICY,
            )
            AudioSourceResolution.NotFound -> ItemResolution.NotFound
            is AudioSourceResolution.Unavailable -> ItemResolution.Unavailable(resolution.reason)
        }
    }
}

/** Sleep sounds loop until something stops them; that is the product, not the engine's default. */
private val SOUND_POLICY = PlaybackPolicy(defaultLooping = DEFAULT_LOOPING)
