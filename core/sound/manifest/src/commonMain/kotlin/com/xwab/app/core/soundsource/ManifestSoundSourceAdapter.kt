package com.xwab.app.core.soundsource

import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.soundsource.port.SoundSourcePort
import com.xwab.app.core.soundsource.port.TrackSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class ManifestSoundSourceAdapter internal constructor(
    entries: List<CatalogEntry>,
) : SoundSourcePort {
    @Inject
    internal constructor() : this(catalogEntries)

    private val sourcesById: Map<TrackId, TrackSource> = entries.associate { entry ->
        entry.music.id to TrackSource(entry.cacheFileName, entry.httpsUrl)
    }

    override val cacheFileNames: Set<String> = entries.mapTo(mutableSetOf()) { it.cacheFileName }

    override fun sourceFor(trackId: TrackId): TrackSource? = sourcesById[trackId]
}
