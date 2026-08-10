@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.sounds

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.feature.category.navigation.navigateToCategory
import com.xwab.app.feature.player.navigation.navigateToPlayer
import com.xwab.app.feature.sounds.navigation.SoundsRoute
import org.koin.compose.viewmodel.koinViewModel

/**
 * Where this feature's routes turn into screens.
 *
 * The two `navigateTo` calls below are the whole of what this feature knows about the rest of the
 * app: extension functions published by other features' navigation modules, never their screens.
 */
internal fun EntryProviderScope<NavKey>.soundsEntry(navigator: Navigator) {
    entry<SoundsRoute> {
        SoundsScreenRoute(
            onCategoryClick = navigator::navigateToCategory,
            // The route carries a plain id; the screen deals in `TrackId`. This is the seam.
            onMusicClick = { trackId -> navigator.navigateToPlayer(trackId.value) },
            viewModel = koinViewModel(),
        )
    }
}
