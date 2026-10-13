package com.xwab.app.core.rehearsal.port

// Explicit because this is common code: `kotlin.jvm.*` is a default import on the JVM only, so
// Android compiles this file without the import and the Kotlin/Native targets do not.
import kotlin.jvm.JvmInline
import kotlinx.coroutines.flow.Flow

/**
 * What this content type is called when something is played.
 *
 * It leaves the process as the engine source id's prefix, so from the first release on it is a
 * stored name rather than a constant.
 */
public const val REHEARSAL_PLAYBACK_KIND: String = "rehearsal"

/** The longest script the listener can keep: a few paragraphs, well inside what a device voice reads. */
public const val REHEARSAL_SCRIPT_MAX_LENGTH: Int = 2_000

/**
 * One wording of the listener's script, and the playback item it is.
 *
 * Each wording is its own item. The session resumes an item it already holds rather than looking
 * it up again, so if every wording shared one id, a script changed after a play would resume the
 * old one's track. The id follows the text instead, and a new wording is a new item.
 */
@JvmInline
value class RehearsalScriptId(val value: String) {
    init {
        require(value.isNotBlank()) { "A script id cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * The listener's rewritten dream as it is stored on this device.
 *
 * @param id the item to play it as; it changes whenever [text] does.
 */
data class RehearsalScript(val id: RehearsalScriptId, val text: String) {
    init {
        require(text.isNotBlank()) { "A script says something." }
    }
}

/**
 * The listener's rewritten nightmare, kept on this device and nowhere else, and played through the
 * session as an item of [REHEARSAL_PLAYBACK_KIND].
 *
 * There is one script at a time: imagery rehearsal works on one dream until it changes.
 */
interface RehearsalPort {
    /** The stored script, or null when none has been written. */
    fun observeScript(): Flow<RehearsalScript?>

    /**
     * Replaces the script with [text], trimmed; a blank text removes it. Text longer than
     * [REHEARSAL_SCRIPT_MAX_LENGTH] is refused with an exception, as a caller's mistake.
     * Cancellation propagates.
     */
    suspend fun save(text: String): RehearsalSaveResult
}

enum class RehearsalSaveResult {
    Saved,

    /** The device's storage refused the write; what was there before is unchanged. */
    Unavailable,
}
