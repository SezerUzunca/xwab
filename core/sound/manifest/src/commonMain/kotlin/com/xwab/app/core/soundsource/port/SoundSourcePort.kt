package com.xwab.app.core.soundsource.port

import com.xwab.app.core.sound.port.TrackId

/** The physical HTTPS source and stable cache name for one sound. */
public data class TrackSource(
    public val cacheFileName: String,
    public val httpsUrl: String,
)

/** Supplies physical sound sources without exposing the manifest implementation. */
public interface SoundSourcePort {
    /** The source for [trackId], or `null` when the catalog has no such sound. */
    public fun sourceFor(trackId: TrackId): TrackSource?

    /** Every cache file name still referenced by the current catalog. */
    public val cacheFileNames: Set<String>
}
