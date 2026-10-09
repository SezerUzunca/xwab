package com.xwab.app

import androidx.compose.ui.window.ComposeUIViewController
import com.xwab.app.composition.AppEntriesFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import platform.UIKit.UIViewController

/**
 * [App] in a view controller, which is how UIKit hosts it: the iOS counterpart of the Android
 * Activity's `setContent`. The composition root calls it with what its graphs build.
 */
fun AppViewController(viewModelFactory: MetroViewModelFactory, entriesFactory: AppEntriesFactory): UIViewController =
    ComposeUIViewController { App(viewModelFactory, entriesFactory) }
