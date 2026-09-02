package com.xwab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import com.xwab.app.composition.DefaultAppComponent
import org.koin.mp.KoinPlatformTools

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Retained across configuration changes, so every descendant component keeps its state
        // without needing its own retention story the way a ViewModel used to provide for free.
        val root = retainedComponent { componentContext ->
            DefaultAppComponent(componentContext, koin = KoinPlatformTools.defaultContext().get())
        }

        setContent {
            App(root)
        }
    }
}
