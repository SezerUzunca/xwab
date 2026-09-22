package com.xwab.app.core.session

import com.xwab.app.core.session.port.PlaybackItemId

/**
 * How a [PlaybackItemId] travels through `core:playback`, which identifies a source by plain string
 * and knows nothing about kinds.
 *
 * The kind is the prefix. It is what stops a sound and a story with the same raw id from looking
 * like one source to the engine — and to the session, which compares the engine's current id
 * against what was asked for and would otherwise send `Play` for a story while a sound of that name
 * is attached.
 *
 * These ids leave the process: on Android the playback service outlives the app, so a reconnect
 * reads back ids an earlier build wrote. That is why each content module treats its kind as a
 * stored name rather than a constant it may rename.
 */
internal fun PlaybackItemId.toEngineId(): String = "$kind:$value"

/**
 * The item an engine source id names, or `null` when it names nothing this session can act on.
 *
 * An id with no separator is not a playback item. It used to be read as a sound — an upgrade path
 * for a media service still running from a build that wrote bare track ids — and that could not
 * survive kinds becoming open: there is no longer a module here that knows "sound" is the one to
 * guess. Nothing is lost that was not already unreachable, because no released build ever wrote a
 * bare id; a service still holding one now reports nothing attached rather than the wrong thing.
 *
 * A prefix no content module has registered comes back as the kind it claims to be. The session
 * finds no resolver for it and answers `ItemNotFound`, which is the honest report for a content
 * type this build no longer has.
 */
internal fun playbackItemIdOf(engineId: String): PlaybackItemId? {
    val kind = engineId.substringBefore(':', missingDelimiterValue = "")
    val value = engineId.substringAfter(':', missingDelimiterValue = "")

    return if (kind.isBlank() || value.isBlank()) null else PlaybackItemId(kind, value)
}
