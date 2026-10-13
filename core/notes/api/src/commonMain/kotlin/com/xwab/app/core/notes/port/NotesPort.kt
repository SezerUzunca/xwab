package com.xwab.app.core.notes.port

import kotlinx.coroutines.flow.Flow

/**
 * The listener's own short notes, kept on this device and nowhere else.
 *
 * Each note lives under a key its caller owns, as favourites live under a caller's namespace, so
 * this module knows nothing of what a note is for. A key is a name on disk: renaming one strands
 * what was written under it.
 */
interface NotesPort {
    /** The note under [key], or an empty text when there is none. */
    fun observe(key: String): Flow<String>

    /** Replaces the note under [key]; an empty or blank text removes it. Cancellation propagates. */
    suspend fun save(key: String, text: String): NoteSaveResult
}

enum class NoteSaveResult {
    Saved,

    /** The device's storage refused the write; what was there before is unchanged. */
    Unavailable,
}
