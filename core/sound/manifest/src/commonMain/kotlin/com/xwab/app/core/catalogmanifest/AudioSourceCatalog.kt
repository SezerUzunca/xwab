package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.TrackId

/** Where one track's audio physically comes from. */
data class TrackSource(
    val cacheFileName: String,
    val httpsUrl: String,
)

/**
 * The manifest's second port: the physical source behind each track, read by `core:sound:delivery`
 * and by nothing else.
 *
 * Kept apart from `MusicCatalogRepository` because the two answer to different callers — and kept
 * in a different *module* from it for the same reason. A screen depends on `core:sound:catalog` for the
 * repository; if this port lived there too, a screen could resolve it out of the container and read
 * [TrackSource.httpsUrl] whatever the documentation said. It lives here, no feature declares this
 * module, and `checkArchitecture` rule 4 fails the build on one that tries.
 */
interface AudioSourceCatalog {
    /** The source for [musicId], or `null` when the catalog holds no such track. */
    fun sourceFor(musicId: TrackId): TrackSource?

    /**
     * Every cache file name the catalog still refers to, which is also the complete list of what
     * the audio cache is allowed to keep: anything else on disk belongs to a version or a track
     * that a later build has left behind.
     */
    val cacheFileNames: Set<String>
}

internal class ManifestAudioSourceCatalog(
    entries: List<CatalogEntry> = catalogEntries,
) : AudioSourceCatalog {
    private val sourcesById: Map<TrackId, TrackSource> = entries.associate { entry ->
        entry.music.id to TrackSource(entry.cacheFileName, entry.httpsUrl)
    }

    override val cacheFileNames: Set<String> = entries.mapTo(mutableSetOf()) { it.cacheFileName }

    override fun sourceFor(musicId: TrackId): TrackSource? = sourcesById[musicId]
}
