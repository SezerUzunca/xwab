package com.xwab.app.core.guide.port

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
public const val GUIDE_PLAYBACK_KIND: String = "guide"

/** The identity of one relaxation guide. It refuses a blank value, as every content id here does. */
@JvmInline
value class GuideId(val value: String) {
    init {
        require(value.isNotBlank()) { "A guide id cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * One spoken relaxation guide, as a screen needs to know it. Its script stays private to this
 * module, as a sound's address does; a screen plays the guide through the session.
 *
 * @param minutes roughly how long it runs. The device's voice sets the exact pace, so this is a
 *   promise of "about", which is how a screen should put it.
 * @param languageTag the BCP 47 tag of the voice that speaks it, such as `en-US`.
 * @param use when it is meant to be heard, which decides the screen that offers it.
 */
data class Guide(
    val id: GuideId,
    val title: String,
    val description: String,
    val minutes: Int,
    val languageTag: String,
    val use: GuideUse,
) {
    init {
        require(title.isNotBlank()) { "A guide needs a title: ${id.value}" }
        require(description.isNotBlank()) { "A guide needs a description: ${id.value}" }
        require(minutes > 0) { "A guide runs for some time: ${id.value}" }
        require(languageTag.isNotBlank()) { "A guide needs a voice language: ${id.value}" }
    }
}

/** When a guide is meant to be heard. A screen offers the guides of its own use and no others. */
enum class GuideUse {
    /** Lying down to sleep: relaxation and imagery that end in rest. */
    WindDown,

    /**
     * Up and out of bed after lying awake: something calm and a little dull to listen to until
     * sleepiness returns. It never asks the listener to lie down or close their eyes.
     */
    OutOfBed,
}

/** Read access to the guides. Their scripts and the spoken tracks stay private to this module. */
interface GuidePort {
    fun observeGuides(): Flow<List<Guide>>
    fun observeGuide(guideId: GuideId): Flow<Guide?>
}
