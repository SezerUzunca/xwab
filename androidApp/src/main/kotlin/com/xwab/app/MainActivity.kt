package com.xwab.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
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

        val appGraph = (application as MainApplication).appGraph

        setContent {
            App(appGraph)
        }
    }
}
