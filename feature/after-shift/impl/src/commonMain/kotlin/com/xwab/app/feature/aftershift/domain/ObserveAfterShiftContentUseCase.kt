package com.xwab.app.feature.aftershift.domain

import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuidePort
import com.xwab.app.core.guide.port.GuideUse
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class AfterShiftContent(val guides: List<Guide>, val playback: PlaybackSummary)

/**
 * The wind-down guides beside the session, so each guide's control shows what the session is doing
 * with it. A guide meant for out of bed belongs to the screen for lying awake, not to this one.
 */
@Inject
internal class ObserveAfterShiftContentUseCase(
    private val guidePort: GuidePort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(): Flow<AfterShiftContent> = combine(
        guidePort.observeGuides(),
        playbackPort.playback,
    ) { guides, playback -> AfterShiftContent(guides.filter { it.use == GuideUse.WindDown }, playback) }
}
