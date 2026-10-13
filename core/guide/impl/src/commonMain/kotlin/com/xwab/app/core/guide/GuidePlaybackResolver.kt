// This module answers the session's resolver contract, so it opts in to it. It never calls the
// session itself: `checkArchitecture` keeps a contributor from also being a consumer.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.guide

import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuidePort
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.NoiseBed
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

/**
 * Turns a guide into a track the platform player can open: its script, spoken by the device's voice
 * through speech, over a soft noise bed.
 *
 * A guide's words never change, so speech makes its track once per voice and hands the same file
 * back afterwards; only the first play waits for it. The bed masks a day's household and street
 * sounds and keeps the pauses from sounding like a dropped connection; it runs on for a while after
 * the last line and fades out, so a guide ends gently rather than on a word.
 *
 * An unknown guide is `NotFound`; one speech could not voice is `Unavailable`, which the screen
 * offers to retry.
 */
@SingleIn(GuideScope::class)
@Inject
internal class GuidePlaybackResolver internal constructor(
    private val catalog: GuidePort,
    scripts: List<GuideScript>,
    private val speech: SpeechPort,
) : PlaybackItemResolver {
    init {
        val duplicates = scripts.groupBy(GuideScript::guideId).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Guide scripts must be unique: ${duplicates.joinToString()}" }
    }

    private val scriptsByGuide = scripts.associateBy(GuideScript::guideId)

    override suspend fun resolve(value: String): ItemResolution {
        val guide = catalog.observeGuide(GuideId(value)).first()
        val script = scriptsByGuide[value]
        return when {
            guide == null -> ItemResolution.NotFound
            // A defensive answer for a manifest gap, which the manifest test is there to prevent.
            script == null -> ItemResolution.Unavailable("the guide has no script")
            else -> when (val result = speech.render(requestFor(guide, script))) {
                is SpeechResult.Rendered -> ItemResolution.Resolved(
                    uri = result.path,
                    title = guide.title,
                    // A guide is listed and announced under the same name; it has no second one.
                    displayName = guide.title,
                    artist = null,
                    policy = GUIDE_POLICY,
                )
                is SpeechResult.Unavailable -> ItemResolution.Unavailable(result.reason)
            }
        }
    }

    private fun requestFor(guide: Guide, script: GuideScript) = SpeechRequest(
        folder = GUIDE_PLAYBACK_KIND,
        name = guide.id.value,
        languageTag = guide.languageTag,
        segments = listOf(SpeechSegment.Pause(OPENING_SECONDS)) +
            script.lines.flatMap { line ->
                listOf(SpeechSegment.Spoken(line.text), SpeechSegment.Pause(line.pauseSeconds))
            } +
            SpeechSegment.Pause(CLOSING_SECONDS),
        bed = GUIDE_BED,
        // The same words every time: made once, then handed back.
        reuse = true,
    )
}

/** A guide plays once and ends, as a story does. */
private val GUIDE_POLICY = PlaybackPolicy(looping = false)

/** Soft enough to sit well under the voice; slow to rise and slow to fall. */
private val GUIDE_BED = NoiseBed(level = 0.035, fadeSeconds = 8.0)

/** A moment of the bed alone before the first word. */
private const val OPENING_SECONDS = 3.0

/** How long the bed runs on after the last line, fading out at its end. */
internal const val CLOSING_SECONDS = 45.0
