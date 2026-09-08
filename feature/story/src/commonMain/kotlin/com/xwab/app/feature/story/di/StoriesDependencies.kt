package com.xwab.app.feature.story.di

import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.story.port.StoryCatalogPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The two ports this screen reads — two, not three: there is no favorites port for stories, and
 * the manifest that knows where one streams from is the session's business.
 */
@SingleIn(AppScope::class)
@Inject
class StoriesDependencies(
    internal val storyCatalogPort: StoryCatalogPort,
    internal val playbackPort: PlaybackPort,
)
