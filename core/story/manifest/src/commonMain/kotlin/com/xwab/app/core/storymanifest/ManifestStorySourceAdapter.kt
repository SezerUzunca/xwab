package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.storysource.port.StorySourcePort
import com.xwab.app.core.storysource.port.StoryStreamSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class ManifestStorySourceAdapter internal constructor(
    entries: List<StoryEntry>,
) : StorySourcePort {
    @Inject
    internal constructor() : this(storyManifest)

    private val sourcesById: Map<StoryId, StoryStreamSource> = entries.associate { entry ->
        entry.story.id to StoryStreamSource(entry.httpsUrl)
    }

    override fun sourceFor(storyId: StoryId): StoryStreamSource? = sourcesById[storyId]
}
