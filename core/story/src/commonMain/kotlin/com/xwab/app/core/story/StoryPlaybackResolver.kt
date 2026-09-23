// This module answers the session's resolver contract, so it opts in to it. It never calls the
// session itself: `checkArchitecture` keeps a contributor from also being a consumer.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.story

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.StringKey
import kotlinx.coroutines.flow.first

/**
 * Pairs story metadata with this module's private stream manifest.
 *
 * Stories stream over HTTPS through the platform player. Source lookup and playback defaults are
 * story policy; this resolver publishes the resulting URI and metadata through the resolution port.
 *
 * An unknown catalog id is `NotFound`; `Unavailable` remains a defensive answer for a
 * catalog/source mismatch, which this module's completeness test is there to prevent.
 */
@ContributesIntoMap(AppScope::class)
@StringKey(STORY_PLAYBACK_KIND)
internal class StoryPlaybackResolver internal constructor(
    private val catalog: StoryPort,
    sources: List<StorySource>,
) : PlaybackItemResolver {
    @Inject
    internal constructor(catalog: StoryPort) : this(catalog, storySourceManifest)

    init {
        val duplicates = sources.groupBy(StorySource::itemId).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Story source ids must be unique: ${duplicates.joinToString()}" }
    }

    private val sourcesById = sources.associateBy(StorySource::itemId)

    override suspend fun resolve(value: String): ItemResolution {
        val storyId = StoryId(value)
        val story = catalog.observeStory(storyId).first() ?: return ItemResolution.NotFound
        val source = sourcesById[value]
            ?: return ItemResolution.Unavailable("story source is missing")

        return ItemResolution.Resolved(
            uri = source.httpsUrl,
            title = story.title,
            // A story is listed and announced under the same name; it has no second one.
            displayName = story.title,
            artist = story.narrator,
            policy = STORY_POLICY,
        )
    }
}

/** A story that repeats has not ended, it has started again. The listener can still turn it on. */
private val STORY_POLICY = PlaybackPolicy(defaultLooping = false)
