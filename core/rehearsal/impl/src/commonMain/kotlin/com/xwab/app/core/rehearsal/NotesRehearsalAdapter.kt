package com.xwab.app.core.rehearsal

import com.xwab.app.core.notes.port.NoteSaveResult
import com.xwab.app.core.notes.port.NotesPort
import com.xwab.app.core.rehearsal.port.REHEARSAL_SCRIPT_MAX_LENGTH
import com.xwab.app.core.rehearsal.port.RehearsalPort
import com.xwab.app.core.rehearsal.port.RehearsalSaveResult
import com.xwab.app.core.rehearsal.port.RehearsalScript
import com.xwab.app.core.rehearsal.port.RehearsalScriptId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Keeps the script as a note under this module's own key, so it stays on the device beside the
 * listener's other notes, and names each wording by its text.
 */
@SingleIn(RehearsalScope::class)
@Inject
internal class NotesRehearsalAdapter internal constructor(private val notes: NotesPort) : RehearsalPort {
    override fun observeScript(): Flow<RehearsalScript?> = notes.observe(SCRIPT_NOTE_KEY).map { stored ->
        stored.trim().takeIf { it.isNotEmpty() }?.let { text -> RehearsalScript(scriptIdOf(text), text) }
    }

    override suspend fun save(text: String): RehearsalSaveResult {
        val trimmed = text.trim()
        require(trimmed.length <= REHEARSAL_SCRIPT_MAX_LENGTH) {
            "A script is at most $REHEARSAL_SCRIPT_MAX_LENGTH characters: ${trimmed.length}"
        }
        return when (notes.save(SCRIPT_NOTE_KEY, trimmed)) {
            NoteSaveResult.Saved -> RehearsalSaveResult.Saved
            NoteSaveResult.Unavailable -> RehearsalSaveResult.Unavailable
        }
    }
}

/**
 * The note the script is kept under. It is a name on disk: renaming it strands every script, so it
 * changes only with a migration; `RehearsalTest` pins it.
 */
internal const val SCRIPT_NOTE_KEY = "rehearsal"

/**
 * A wording's id: a 64-bit FNV-1a hash of its text, in hex. Stable across processes and platforms,
 * which a saved engine source id needs, and different for any edit a listener makes in practice.
 */
internal fun scriptIdOf(text: String): RehearsalScriptId {
    var hash = FNV_OFFSET
    text.encodeToByteArray().forEach { byte ->
        hash = (hash xor byte.toUByte().toULong()) * FNV_PRIME
    }
    return RehearsalScriptId(hash.toString(radix = 16).padStart(HASH_DIGITS, '0'))
}

private const val FNV_OFFSET = 0xcbf29ce484222325uL
private const val FNV_PRIME = 0x100000001b3uL
private const val HASH_DIGITS = 16
