@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.story.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.story.api.navigation.StoriesRoute
import com.xwab.app.feature.story.impl.StoriesScreenRoute
import org.koin.compose.viewmodel.koinViewModel

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.storiesEntry() {
    entry<StoriesRoute> {
        StoriesScreenRoute(viewModel = koinViewModel())
    }
}
