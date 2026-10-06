package com.xwab.app.core.playback.platform

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.xwab.app.core.playback.timer.SleepTimerClock
import com.xwab.app.core.playback.timer.TickScheduler
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides

/**
 * What both Android graphs need for a sleep timer: the engine's [AndroidPlaybackGraph] for its
 * countdown, the service's [PlaybackServiceGraph] for the timer it owns. Both graphs include it.
 */
@BindingContainer
internal interface AndroidPlaybackBindings {
    @Binds val HandlerTickScheduler.bindTickScheduler: TickScheduler

    companion object {
        /** A main-looper handler; unscoped, so each scheduler posts through its own. */
        @Provides
        fun provideMainHandler(): Handler = Handler(Looper.getMainLooper())

        /**
         * `elapsedRealtime`, because the sleep-timer deadline is shared between the engine and
         * [PlaybackService], which owns the timer; it also keeps running while the device sleeps.
         */
        @Provides
        fun provideSleepTimerClock(): SleepTimerClock = SleepTimerClock(SystemClock::elapsedRealtime)
    }
}
