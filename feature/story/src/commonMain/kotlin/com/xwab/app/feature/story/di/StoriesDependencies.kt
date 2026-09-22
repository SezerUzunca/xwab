package com.xwab.app.feature.story.di

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The two ports this screen reads: story metadata and the playback session.
 *
 * `StoryPort` is metadata only. Physical addresses and the story resolver stay internal to
 * `:core:story`. This screen names a story to [PlaybackPort]; the session delegates resolution
 * through the shared playback resolver contract.
 */
@SingleIn(AppScope::class)
@Inject
class StoriesDependencies(
    internal val storyPort: StoryPort,
    internal val playbackPort: PlaybackPort,
)
