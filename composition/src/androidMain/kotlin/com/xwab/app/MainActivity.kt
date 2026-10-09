package com.xwab.app

import android.app.Activity
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xwab.app.composition.AppEntryGraphs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

/**
 * Constructed by Metro: MetroX's `AppComponentFactory` asks the application graph for it, so it
 * receives what it needs instead of reaching into the application object for the graph.
 */
@ContributesIntoMap(AppScope::class, binding<Activity>())
@ActivityKey
@Inject
class MainActivity(private val viewModelFactory: MetroViewModelFactory) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Stated outright rather than left to `enableEdgeToEdge()`'s default, which follows the
        // *device's* theme. This app has no light theme — every screen paints the same dark
        // gradient — so on a phone set to light mode the default put dark status-bar icons on a
        // dark background and made them all but invisible.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            App(viewModelFactory, AppEntryGraphs)
        }
    }

    /**
     * The phone's volume keys are the app's only volume control, so they must reach the media
     * stream even while nothing plays — otherwise they change the ringer. The stream matches the
     * player's `USAGE_MEDIA` attributes; `onResume` is where the official media guide makes this call.
     */
    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }
}
