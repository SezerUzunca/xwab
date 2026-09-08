package com.xwab.app.feature.story.di

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The two ports this screen reads — two, not three, because stories have no favorites port.
 *
 * `StoryPort` carries the stream address next to the metadata, so unlike the split this replaced,
 * nothing at the module boundary stops a screen from asking for one. Only the session does, and a
 * screen has no use for an address it would only hand back through [PlaybackPort].
 */
@SingleIn(AppScope::class)
@Inject
class StoriesDependencies(
    internal val storyPort: StoryPort,
    internal val playbackPort: PlaybackPort,
)
