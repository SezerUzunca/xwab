package com.xwab.app

import androidx.compose.ui.window.ComposeUIViewController
import com.xwab.app.di.createAppGraph

/** Built once for the process: the graph outlives every view controller made from it. */
private val appGraph by lazy { createAppGraph() }

fun MainViewController() = ComposeUIViewController { App(appGraph) }
