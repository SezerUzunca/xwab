package com.xwab.app

import com.xwab.app.composition.AppEntryGraphs
import platform.UIKit.UIViewController

/** The view controller each scene of the iOS app shows: the shell's UI over [runtime]'s graph. */
fun MainViewController(runtime: AppRuntime): UIViewController =
    AppViewController(runtime.graph.metroViewModelFactory, AppEntryGraphs)
