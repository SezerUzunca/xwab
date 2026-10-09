package com.xwab.app.composition

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.ResultEventBus
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.rememberNavigationState
import com.xwab.app.ui.AppNavigationDisplay
import com.xwab.app.ui.rememberTabEntries

/**
 * Application wiring stays here; scenes, entry decorators and saved stacks live in navigation.
 *
 * The host owns its navigator and result bus, built once per restored state. [entriesFactory] turns
 * that navigator into the features' entries and bar; the composition root supplies it, because only
 * the root sees the implementations they come from.
 */
@Composable
internal fun AppNavigationHost(entriesFactory: AppEntriesFactory) {
    val state = rememberNavigationState()
    val resultEventBus = remember(state) { ResultEventBus() }
    val navigator = remember(state) { Navigator(state, resultEventBus) }
    val entries = remember(navigator) { entriesFactory.create(navigator) }
    val provider = remember(entries) { appEntryProvider(entries) }
    AppNavigationDisplay(
        entries = rememberTabEntries(state, provider, ::appEntryMetadata, navigator::goUp, resultEventBus),
        selectedTab = state.topLevelRoute,
        onSelectTab = navigator::selectTab,
        onBack = navigator::goBack,
        nowPlayingBar = {
            entries.nowPlayingBar.Content(
                onOpen = { item -> openPlaybackDetails(item, navigator::openInTab) },
                hiddenFor = state.currentBackStack.last().playbackItem(),
                modifier = Modifier,
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}
