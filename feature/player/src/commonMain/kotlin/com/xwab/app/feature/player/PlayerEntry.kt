@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.player

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.feature.player.navigation.PlayerRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Where this feature's routes turn into screens. */
internal fun EntryProviderScope<NavKey>.playerEntry(navigator: Navigator) {
    entry<PlayerRoute> { route ->
        PlayerScreenRoute(
            onBack = navigator::goBack,
            viewModel = koinViewModel {
                // A route is a serialized wire format, so it carries the plain id and the wrapper
                // goes back on here — the one place this feature handles a bare track string.
                parametersOf(TrackId(route.musicId))
            },
        )
    }
}
