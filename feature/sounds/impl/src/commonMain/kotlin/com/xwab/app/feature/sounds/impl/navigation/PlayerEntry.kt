@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.sounds.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.sounds.api.navigation.PlayerRoute
import com.xwab.app.feature.sounds.impl.PlayerScreenRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.playerEntry(onBack: () -> Unit) {
    entry<PlayerRoute> { route ->
        PlayerScreenRoute(
            onBack = onBack,
            viewModel = koinViewModel {
                // A route is a serialized wire format, so it carries the plain id and the wrapper
                // goes back on here — the one place this feature handles a bare track string.
                parametersOf(TrackId(route.musicId))
            },
        )
    }
}
