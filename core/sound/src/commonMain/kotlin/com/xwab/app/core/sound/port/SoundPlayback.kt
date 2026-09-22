package com.xwab.app.core.sound.port

/**
 * What this content type is called when something is played.
 *
 * A playback item id is a kind and a raw value, and this is the kind half for sounds. It is
 * declared here, beside [TrackId], because naming this content type is this module's business:
 * `:core:session` holds one playback for whatever kind of thing the app has, and knowing that
 * "sound" is one of them is exactly what it must not do.
 *
 * Deliberately not shared with `:core:sources`' own `SOUND_NAMESPACE`, which happens to read the
 * same today. That one namespaces physical addresses and cache filenames; this one names a playback
 * kind. They answer to different modules and are free to diverge.
 *
 * Like the favorites namespace beside it, this leaves the process: it is the prefix the engine
 * source id carries, and on Android the playback service outlives the app, so a reconnect reads
 * back ids an earlier build wrote. From the first release on, changing it detaches playback that
 * is still running.
 */
public const val SOUND_PLAYBACK_KIND: String = "sound"
