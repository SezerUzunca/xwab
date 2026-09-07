package com.xwab.app.core.sounddelivery.port

import com.xwab.app.core.sound.port.TrackId

/** The result of resolving one sound into something the playback engine can open. */
public sealed interface SoundContentResolution {
    /** An absolute app-owned path or a remote HTTPS URI. */
    public data class Resolved(public val uri: String) : SoundContentResolution

    /** The catalog no longer contains the requested sound. */
    public data object NotFound : SoundContentResolution

    /** The sound exists, but no playable source could be produced. */
    public data class Unavailable(public val reason: String?) : SoundContentResolution
}

/** Resolves catalog identities without exposing cache, network, or manifest details. */
public fun interface SoundContentPort {
    public suspend fun resolve(trackId: TrackId): SoundContentResolution
}
