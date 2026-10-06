package com.xwab.app.core.playback.platform

import dev.zacsweers.metro.Qualifier

/**
 * The user agent the application asks its playback to present when streaming, or null when it
 * states none. The value is the application's: each platform's graph reads it from what the app
 * declares (Android's manifest, iOS's Info.plist), so this module only learns that requests should
 * say who is making them, never who that is.
 */
@Qualifier
internal annotation class ApplicationUserAgent
