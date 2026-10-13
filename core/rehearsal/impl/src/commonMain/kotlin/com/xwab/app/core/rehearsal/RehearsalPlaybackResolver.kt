// This module answers the session's resolver contract, so it opts in to it. It never calls the
// session itself: `checkArchitecture` keeps a contributor from also being a consumer.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.rehearsal

import com.xwab.app.core.rehearsal.port.RehearsalPort
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechResult
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

/**
 * Turns the listener's script into a track the platform player can open: their own words, in the
 * device's voice, framed for rehearsal and over a soft noise bed.
 *
 * Only the stored wording resolves. An id naming an earlier wording — one the engine still holds
 * after the script was changed or removed — is `NotFound`, as a removed guide is; one speech could
 * not voice is `Unavailable`, which the screen offers to retry.
 */
@SingleIn(RehearsalScope::class)
@Inject
internal class RehearsalPlaybackResolver internal constructor(
    private val rehearsal: RehearsalPort,
    private val speech: SpeechPort,
) : PlaybackItemResolver {
    override suspend fun resolve(value: String): ItemResolution {
        val script = rehearsal.observeScript().first()
        if (script == null || script.id.value != value) return ItemResolution.NotFound
        return when (val result = speech.render(rehearsalRequest(script.text))) {
            is SpeechResult.Rendered -> ItemResolution.Resolved(
                uri = result.path,
                title = REHEARSAL_TITLE,
                displayName = REHEARSAL_TITLE,
                artist = null,
                policy = REHEARSAL_POLICY,
            )
            is SpeechResult.Unavailable -> ItemResolution.Unavailable(result.reason)
        }
    }
}

/** What the now-playing bar and the media session call the listener's script. */
internal const val REHEARSAL_TITLE = "Your New Dream"

/** A rehearsal is gone through once, awake, and ends. */
private val REHEARSAL_POLICY = PlaybackPolicy(looping = false)
