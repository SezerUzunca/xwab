package com.xwab.app.core.audiodelivery

import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.catalogmanifest.TrackSource

/**
 * A manifest holding exactly the tracks one test cares about.
 *
 * Delivery reads the catalog through a port now, so its tests no longer reach into the shipped
 * manifest and pick a name out of it — which used to tie a cache assertion to whichever track
 * happened to be listed first.
 */
internal class FakeAudioSourceCatalog(
    private val sources: Map<TrackId, TrackSource> = emptyMap(),
) : AudioSourceCatalog {
    override val cacheFileNames: Set<String> =
        sources.values.mapTo(mutableSetOf()) { it.cacheFileName }

    override fun sourceFor(musicId: TrackId): TrackSource? = sources[musicId]
}

/**
 * A catalog that refers to [cacheFileNames] and nothing else, which is all the sweep ever asks of
 * it. The track ids are derived from the names so the fake stays a one-liner at the call site.
 */
internal fun catalogKeeping(vararg cacheFileNames: String) = FakeAudioSourceCatalog(
    cacheFileNames.associate { name ->
        TrackId(name.substringBeforeLast("-v")) to TrackSource(name, "https://example.test/$name")
    },
)
