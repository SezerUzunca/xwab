package com.xwab.app.core.playbackengine.di

import android.content.Context
import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbackengine.platform.createAndroidPlaybackController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * The platform player, for the lifetime of the process.
 *
 * It is no longer released on container teardown the way the Koin binding was: a compile-time
 * graph has no close hook, and the controller outlives every screen either way.
 */
@ContributesTo(AppScope::class)
interface PlaybackAndroidProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun providePlaybackController(context: Context): PlaybackController =
        createAndroidPlaybackController(context)
}
