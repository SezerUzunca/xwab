package com.xwab.app.core.nap

import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapId

/**
 * The naps this build ships. A nap's id is stored on devices — in the engine's source id and the
 * now-playing state — so keep it once it has shipped.
 *
 * Ten to twenty minutes: published studies find short naps restore alertness with little of the
 * grogginess that follows waking from deep sleep, which longer naps reach. Longer naps also make a
 * track of hundreds of megabytes, since a track is uncompressed audio.
 */
internal val naps: List<Nap> = listOf(
    nap(10, "The shortest: a quick lift with the least grogginess."),
    nap(15, "A little longer to settle, still short enough to wake clear."),
    nap(20, "The longest short nap. Lie down as soon as you start."),
)

private fun nap(minutes: Int, description: String) = Nap(
    id = NapId("nap-$minutes"),
    title = "$minutes-Minute Nap",
    description = description,
    minutes = minutes,
    languageTag = ENGLISH,
)

private const val ENGLISH = "en-US"
