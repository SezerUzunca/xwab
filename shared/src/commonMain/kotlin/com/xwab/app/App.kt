package com.xwab.app

import androidx.compose.runtime.Composable
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.ui.XwabApp

/**
 * Platform entry points call this thin, shared application root.
 *
 * The theme is applied here rather than inside `XwabApp`, the way Now in Android applies `NiaTheme`
 * in `MainActivity` and leaves `NiaApp` free of it. That keeps the shell renderable under a
 * different theme in a preview or a screenshot test, and it is the only reason this hop exists.
 */
@Composable
fun App() {
    SleepRelaxTheme {
        XwabApp()
    }
}
