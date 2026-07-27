package com.xwab.app

import androidx.compose.runtime.Composable
import com.xwab.app.navigation.AppNavigation
import com.xwab.app.core.ui.theme.SleepRelaxTheme

@Composable
fun App() {
    SleepRelaxTheme {
        AppNavigation()
    }
}
