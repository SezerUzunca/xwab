package com.xwab.app.feature.cantsleep.domain

import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuidePort
import com.xwab.app.core.guide.port.GuideUse
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class CantSleepContent(val guides: List<Guide>, val playback: PlaybackSummary)

/** The guides meant for out of bed beside the session; the wind-down guides belong to After Shift. */
@Inject
internal class ObserveCantSleepContentUseCase(
    private val guidePort: GuidePort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(): Flow<CantSleepContent> = combine(
        guidePort.observeGuides(),
        playbackPort.playback,
    ) { guides, playback -> CantSleepContent(guides.filter { it.use == GuideUse.OutOfBed }, playback) }
}
