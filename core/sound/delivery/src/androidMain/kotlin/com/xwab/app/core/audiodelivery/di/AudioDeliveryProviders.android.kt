package com.xwab.app.core.audiodelivery.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath

/** The Android cache directory this app is allowed to fill. */
@ContributesTo(AppScope::class)
interface AudioDeliveryAndroidProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun provideAudioCacheRoot(context: Context): AudioCacheRoot =
        AudioCacheRoot(context.cacheDir.resolve("audio-content").absolutePath.toPath())
}
