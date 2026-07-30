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
import com.xwab.app.di.featureSerializers
import com.xwab.app.feature.home.navigation.HomeRoute
import org.koin.compose.navigation3.koinEntryProvider

@Composable
fun AppNavigation() {
    val configuration = remember {
        // Collected from the feature list; the only route this file names is the start one.
        SavedStateConfiguration { serializersModule = featureSerializers }
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
