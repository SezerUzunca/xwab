@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.favorites.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.favorites.api.navigation.FavoritesRoute
import com.xwab.app.feature.favorites.impl.FavoritesScreenRoute
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.favoritesEntry(onMusicClick: (TrackId) -> Unit) {
    entry<FavoritesRoute> {
        FavoritesScreenRoute(
            onMusicClick = onMusicClick,
            viewModel = koinViewModel(),
        )
    }
}
