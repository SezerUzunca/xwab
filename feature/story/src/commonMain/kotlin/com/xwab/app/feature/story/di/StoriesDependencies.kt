package com.xwab.app.feature.story.di

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The two ports this screen reads — two, not three, because stories have no favorites port.
 *
 * `StoryPort` is metadata only. Where a story's audio actually lives is `:core:sources`, which a
 * feature may not depend on at all — so this screen could not ask for an address even if it wanted
 * one, and it does not: it names a story to [PlaybackPort] and the session resolves it.
 *
 * This comment used to say the opposite, from before the addresses were split out, which is the
 * kind of stale signal that invites the next reader to reach for something the boundary has since
 * taken away.
 */
@SingleIn(AppScope::class)
@Inject
class StoriesDependencies(
    internal val storyPort: StoryPort,
    internal val playbackPort: PlaybackPort,
)
