// This module answers the session's resolver contract, so it opts in to it. It never calls the
// session itself: `checkArchitecture` keeps a contributor from also being a consumer.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.nap

import com.xwab.app.core.nap.port.NapId
import com.xwab.app.core.nap.port.NapPort
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
 * Turns a nap into a track the platform player can open: soft noise for the nap's length, then a
 * voice that wakes the listener, made by speech once per voice and handed back afterwards.
 *
 * An unknown nap is `NotFound`; one speech could not voice is `Unavailable`, which the screen
 * offers to retry.
 */
@SingleIn(NapScope::class)
@Inject
internal class NapPlaybackResolver internal constructor(
    private val catalog: NapPort,
    private val speech: SpeechPort,
) : PlaybackItemResolver {
    override suspend fun resolve(value: String): ItemResolution {
        val nap = catalog.observeNap(NapId(value)).first() ?: return ItemResolution.NotFound
        return when (val result = speech.render(napRequest(nap))) {
            is SpeechResult.Rendered -> ItemResolution.Resolved(
                uri = result.path,
                title = nap.title,
                displayName = nap.title,
                artist = null,
                policy = NAP_POLICY,
            )
            is SpeechResult.Unavailable -> ItemResolution.Unavailable(result.reason)
        }
    }
}

/** A nap ends with its wake-up; looping would put the listener back to sleep. */
private val NAP_POLICY = PlaybackPolicy(looping = false)
