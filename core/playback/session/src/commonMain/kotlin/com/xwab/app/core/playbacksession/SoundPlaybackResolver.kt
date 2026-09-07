package com.xwab.app.core.playbacksession

import com.xwab.app.core.playback.port.DEFAULT_LOOPING
import com.xwab.app.core.playback.port.PlaybackKind
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.sounddelivery.port.SoundContentResolution
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
    private val catalog: SoundCatalogPort,
    private val content: SoundContentPort,
) : PlaybackItemResolver {
    override val kind: PlaybackKind = PlaybackKind.SOUND

    override suspend fun resolve(value: String): ItemResolution {
        val trackId = TrackId(value)
        val music = catalog.observeMusic(trackId).first() ?: return ItemResolution.NotFound

        return when (val resolution = content.resolve(trackId)) {
            is SoundContentResolution.Resolved -> ItemResolution.Resolved(
                uri = resolution.uri,
                title = music.playbackTitle,
                artist = music.playbackArtist,
                policy = SOUND_POLICY,
            )
            SoundContentResolution.NotFound -> ItemResolution.NotFound
            is SoundContentResolution.Unavailable -> ItemResolution.Unavailable(resolution.reason)
        }
    }
}

/** Sleep sounds loop until something stops them; that is the product, not the engine's default. */
private val SOUND_POLICY = PlaybackPolicy(defaultLooping = DEFAULT_LOOPING)
