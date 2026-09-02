package com.xwab.app

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.xwab.app.composition.DefaultAppComponent
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

/**
 * Called once for the process, so the root component is built here directly rather than
 * remembered inside the composition — there is no configuration-change cycle to retain it across
 * on iOS. [ApplicationLifecycle] tracks the real `UIApplication` foreground/background state.
 */
fun MainViewController(): UIViewController {
    val root = DefaultAppComponent(
        componentContext = DefaultComponentContext(lifecycle = ApplicationLifecycle()),
        koin = KoinPlatformTools.defaultContext().get(),
    )
    return ComposeUIViewController { App(root) }
}
