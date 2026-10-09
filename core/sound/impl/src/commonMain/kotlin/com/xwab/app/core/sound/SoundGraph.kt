// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.Track
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey
import dev.zacsweers.metro.createGraphFactory

/** The lifetime of this module's own graph: one per application, owned by [SoundGraphHolder]. */
internal object SoundScope

/**
 * This module's own graph: the catalog, the physical sources and the resolver are wired here.
 * Only [SoundPort] and the resolver reach the application graph, through the two adapters below;
 * delivery is the one thing it takes from outside.
 */
@DependencyGraph(SoundScope::class)
internal interface SoundGraph {
    val catalog: SoundPort
    val resolver: PlaybackItemResolver

    @Binds val ManifestSoundCatalogAdapter.bindCatalog: SoundPort
    @Binds val SoundPlaybackResolver.bindResolver: PlaybackItemResolver

    @Provides fun provideTracks(): List<Track> = catalogManifest
    @Provides fun provideCategories(): List<Category> = catalogCategories
    @Provides fun provideSources(): List<SoundSource> = soundSourceManifest

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides delivery: DeliveryPort): SoundGraph
    }
}

/** Builds the module graph once, so the catalog and the resolver share it. */
@SingleIn(AppScope::class)
@Inject
internal class SoundGraphHolder(delivery: DeliveryPort) {
    val graph: SoundGraph = createGraphFactory<SoundGraph.Factory>().create(delivery)
}

/** Hands the module graph's catalog to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class SoundCatalogGraphAdapter(holder: SoundGraphHolder) : SoundPort by holder.graph.catalog

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(SOUND_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class SoundResolverGraphAdapter(
    holder: SoundGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
