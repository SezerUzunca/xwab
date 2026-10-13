package com.xwab.app.core.shuffle

import okio.Path

/**
 * Where this platform keeps spoken words and rendered tracks. Each platform's graph binds its own.
 *
 * A cache rather than storage on purpose: everything under it can be spoken again, so the system
 * may clear it whenever it needs the room.
 */
internal fun interface ShuffleCacheLocation {
    /** Read when a track is rendered, not when the graph is built: finding it can touch the disk. */
    fun root(): Path
}
