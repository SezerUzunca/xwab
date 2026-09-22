package com.xwab.app.core.story

import com.xwab.app.core.resolution.port.ItemResolution
import com.xwab.app.core.resolution.port.PlaybackItemResolver
import com.xwab.app.core.resolution.port.PlaybackPolicy
import com.xwab.app.core.sources.port.STORY_NAMESPACE
import com.xwab.app.core.sources.port.SourcePort
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.StringKey
import kotlinx.coroutines.flow.first

/**
 * What playing a story means: metadata from this module's catalog, an address from the source port.
 *
 * The same two steps as `SoundPlaybackResolver`, with the cache step missing. A sound is resolved
 * through `:core:delivery`, which answers with a local file when there is one and starts a download
 * when there is not. A story has no such module by design: it streams over HTTPS and nothing is
 * kept — which is also why this module, unlike `:core:sound`, never depends on delivery at all.
 *
 * An unknown catalog id is `NotFound`; `Unavailable` remains a defensive answer for a
 * catalog/source mismatch, which the composition root's consistency test is there to prevent.
 */
@ContributesIntoMap(AppScope::class)
@StringKey(STORY_PLAYBACK_KIND)
@Inject
internal class StoryPlaybackResolver(
    private val catalog: StoryPort,
    private val sources: SourcePort,
) : PlaybackItemResolver {

    override suspend fun resolve(value: String): ItemResolution {
        val storyId = StoryId(value)
        val story = catalog.observeStory(storyId).first() ?: return ItemResolution.NotFound
        val source = sources.sourceFor(STORY_NAMESPACE, value)
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
