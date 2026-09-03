package com.xwab.app.core.playbackengine.di

import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbackengine.platform.createIosPlaybackController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** The platform player, for the lifetime of the process. */
@ContributesTo(AppScope::class)
interface PlaybackIosProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun providePlaybackController(): PlaybackController = createIosPlaybackController()
}
