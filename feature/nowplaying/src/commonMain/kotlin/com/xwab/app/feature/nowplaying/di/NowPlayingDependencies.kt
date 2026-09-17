package com.xwab.app.feature.nowplaying.di

import com.xwab.app.core.session.port.PlaybackPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The one port this feature reads.
 *
 * It needs no catalog: the session names what it is on, so a bar over "whatever is playing" never
 * has to ask whether that is a sound or a story.
 */
@SingleIn(AppScope::class)
@Inject
class NowPlayingDependencies(
    internal val playbackPort: PlaybackPort,
)
