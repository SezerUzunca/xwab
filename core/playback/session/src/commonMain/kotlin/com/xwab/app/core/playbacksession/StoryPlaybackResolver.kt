package com.xwab.app.core.playbacksession

import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.story.StoryId
import com.xwab.app.core.storymanifest.StoryStreamCatalog
import kotlinx.coroutines.flow.first

/**
 * Stories: metadata from the catalog, an address from the stream catalog.
 *
 * The same two steps as [SoundPlaybackResolver], with the cache step missing. A sound is resolved
 * through `core:sound:delivery`, which answers with a local file when there is one and starts a
 * download when there is not. A story has no such module by design: it streams over HTTPS and
 * nothing is kept.
 *
 * The shipped manifest pairs every story with a source. `Unavailable` remains a defensive answer
 * for a catalog/source mismatch, while an unknown catalog id is `NotFound`.
 */
internal class StoryPlaybackResolver(
    private val catalog: StoryCatalogRepository,
    private val streams: StoryStreamCatalog,
) : PlaybackItemResolver {
    override val kind: PlaybackKind = PlaybackKind.STORY

    override suspend fun resolve(value: String): ItemResolution {
        val storyId = StoryId(value)
        val story = catalog.observeStory(storyId).first() ?: return ItemResolution.NotFound
        val source = streams.sourceFor(storyId)
            ?: return ItemResolution.Unavailable("story source is missing")

        return ItemResolution.Resolved(
            uri = source.httpsUrl,
            title = story.title,
            artist = story.narrator,
            policy = STORY_POLICY,
        )
    }
}

/** A story that repeats has not ended, it has started again. The listener can still turn it on. */
private val STORY_POLICY = PlaybackPolicy(defaultLooping = false)
