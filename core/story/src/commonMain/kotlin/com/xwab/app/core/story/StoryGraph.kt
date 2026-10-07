// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.story

import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey
import dev.zacsweers.metro.createGraph

/** The lifetime of this module's own graph: one per application, owned by [StoryGraphHolder]. */
internal object StoryScope

/**
 * This module's own graph: the catalog and the resolver are wired here, and only [StoryPort] and
 * the resolver reach the application graph, through the two adapters below. It needs nothing from
 * outside.
 */
@DependencyGraph(StoryScope::class)
internal interface StoryGraph {
    val catalog: StoryPort
    val resolver: PlaybackItemResolver

    @Binds val ManifestStoryCatalogAdapter.bindCatalog: StoryPort
    @Binds val StoryPlaybackResolver.bindResolver: PlaybackItemResolver

    @Provides fun provideStories(): List<Story> = storyManifest
    @Provides fun provideSources(): List<StorySource> = storySourceManifest
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class StoryGraphHolder {
    val graph: StoryGraph = createGraph<StoryGraph>()
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class StoryCatalogGraphAdapter(holder: StoryGraphHolder) : StoryPort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(STORY_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class StoryResolverGraphAdapter(
    holder: StoryGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
