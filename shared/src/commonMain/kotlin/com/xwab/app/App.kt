package com.xwab.app

import androidx.compose.runtime.Composable
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import com.xwab.app.navigation.AppNavigation

@Composable
fun App() {
    SleepRelaxTheme {
        AppNavigation()
    }
}
