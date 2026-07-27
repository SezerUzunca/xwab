package com.xwab.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

class Navigator(
    private val backStack: MutableList<NavKey>,
) {
    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator is not available")
}

@Composable
fun rememberNavigator(backStack: MutableList<NavKey>): Navigator = remember(backStack) {
    Navigator(backStack)
}
