@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.story

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.feature.story.navigation.StoriesRoute
import org.koin.compose.viewmodel.koinViewModel

/**
 * Where this feature's routes turn into screens.
 *
 * The navigator goes unused: this feature owns one route, it is a tab's root, and every story is
 * played from its own row. There is nothing above it to go back to and nowhere for it to route.
 * The parameter stays so that every feature's entries have the one shape the shell calls.
 */
internal fun EntryProviderScope<NavKey>.storiesEntry(
    @Suppress("UNUSED_PARAMETER") navigator: Navigator,
) {
    entry<StoriesRoute> {
        StoriesScreenRoute(viewModel = koinViewModel())
    }
}
