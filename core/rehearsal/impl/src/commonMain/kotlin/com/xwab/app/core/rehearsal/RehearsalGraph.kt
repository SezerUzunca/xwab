// The resolver bridge answers the session's resolver contract, so this file opts in to it.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.rehearsal

import com.xwab.app.core.notes.port.NotesPort
import com.xwab.app.core.rehearsal.port.REHEARSAL_PLAYBACK_KIND
import com.xwab.app.core.rehearsal.port.RehearsalPort
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.SpeechPort
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

/** The lifetime of this module's own graph: one per application, owned by [RehearsalGraphHolder]. */
internal object RehearsalScope

/**
 * This module's own graph: the script's storage and the resolver are wired here. Only
 * [RehearsalPort] and the resolver reach the application graph, through the two adapters below;
 * notes and speech are what it takes from outside.
 */
@DependencyGraph(RehearsalScope::class)
internal interface RehearsalGraph {
    val rehearsal: RehearsalPort
    val resolver: PlaybackItemResolver

    @Binds val NotesRehearsalAdapter.bindRehearsal: RehearsalPort
    @Binds val RehearsalPlaybackResolver.bindResolver: PlaybackItemResolver

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides notes: NotesPort, @Provides speech: SpeechPort): RehearsalGraph
    }
}

/** Builds the module graph once, so the port and the resolver read the same script. */
@SingleIn(AppScope::class)
@Inject
internal class RehearsalGraphHolder(notes: NotesPort, speech: SpeechPort) {
    val graph: RehearsalGraph = createGraphFactory<RehearsalGraph.Factory>().create(notes, speech)
}

/** Hands the module graph's port to the application graph. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class RehearsalGraphAdapter(holder: RehearsalGraphHolder) : RehearsalPort by holder.graph.rehearsal

/** Registers the module graph's resolver in the session's map, under this module's kind. */
@ContributesIntoMap(AppScope::class)
@StringKey(REHEARSAL_PLAYBACK_KIND)
@SingleIn(AppScope::class)
@Inject
internal class RehearsalResolverGraphAdapter(
    holder: RehearsalGraphHolder,
) : PlaybackItemResolver by holder.graph.resolver
