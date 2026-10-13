package com.xwab.app.core.guide

import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuidePort
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves guide metadata from one immutable manifest. */
@SingleIn(GuideScope::class)
@Inject
internal class ManifestGuideCatalogAdapter internal constructor(guides: List<Guide>) : GuidePort {
    init {
        val duplicates = guides.groupBy(Guide::id).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Guide ids must be unique: ${duplicates.joinToString { it.value }}" }
    }

    private val allGuides = flowOf(guides)

    override fun observeGuides(): Flow<List<Guide>> = allGuides
    override fun observeGuide(guideId: GuideId): Flow<Guide?> =
        allGuides.map { values -> values.find { it.id == guideId } }
}
