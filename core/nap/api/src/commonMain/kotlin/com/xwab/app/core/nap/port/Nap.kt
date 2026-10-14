package com.xwab.app.core.nap.port

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
public const val NAP_PLAYBACK_KIND: String = "nap"

/** The identity of one nap length. It refuses a blank value, as every content id here does. */
@JvmInline
value class NapId(val value: String) {
    init {
        require(value.isNotBlank()) { "A nap id cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * One fixed-length nap: soft noise for [minutes], then a voice that wakes the listener.
 *
 * The length is fixed and nothing is measured: the wake-up comes [minutes] after play starts,
 * however long the listener took to fall asleep. It is a track the media player plays, not an
 * alarm; a screen offering it says so.
 *
 * @param languageTag the BCP 47 tag of the voice that speaks it, such as `en-US`.
 */
data class Nap(
    val id: NapId,
    val title: String,
    val description: String,
    val minutes: Int,
    val languageTag: String,
) {
    init {
        require(title.isNotBlank()) { "A nap needs a title: ${id.value}" }
        require(description.isNotBlank()) { "A nap needs a description: ${id.value}" }
        require(minutes > 0) { "A nap lasts some time: ${id.value}" }
        require(languageTag.isNotBlank()) { "A nap needs a voice language: ${id.value}" }
    }
}

/** Read access to the naps. What each says and the spoken tracks stay private to this module. */
interface NapPort {
    fun observeNaps(): Flow<List<Nap>>
    fun observeNap(napId: NapId): Flow<Nap?>
}
