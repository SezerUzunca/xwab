@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.xwab.app.core.navigation.LocalNavigator
import com.xwab.app.core.navigation.rememberNavigator
import com.xwab.app.feature.home.navigation.HomeRoute
import com.xwab.app.feature.category.navigation.categoryNavigationSerializers
import com.xwab.app.feature.home.navigation.homeNavigationSerializers
import com.xwab.app.feature.player.navigation.playerNavigationSerializers
import kotlinx.serialization.modules.SerializersModule
import org.koin.compose.navigation3.koinEntryProvider

@Composable
fun AppNavigation() {
    val configuration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                include(homeNavigationSerializers)
                include(categoryNavigationSerializers)
                include(playerNavigationSerializers)
            }
        }
    }
    val backStack = rememberNavBackStack(configuration, HomeRoute)
    val navigator = rememberNavigator(backStack)

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            onBack = navigator::goBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = koinEntryProvider(),
        )
    }
}
