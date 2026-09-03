package com.xwab.app.feature.story.impl.di

import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.story.StoryCatalogRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The two ports this screen reads — two, not three: there is no favorites port for stories, and
 * the manifest that knows where one streams from is the session's business.
 */
@SingleIn(AppScope::class)
@Inject
class StoryDependencies(
    internal val storyCatalog: StoryCatalogRepository,
    internal val playbackCoordinator: PlaybackCoordinator,
)
