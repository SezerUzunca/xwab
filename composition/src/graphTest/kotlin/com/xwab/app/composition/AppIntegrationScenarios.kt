@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.xwab.app.composition

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.xwab.app.App
import com.xwab.app.TestRootOwner
import com.xwab.app.composition.OfflineCatalog.Companion.CATEGORY
import com.xwab.app.composition.OfflineCatalog.Companion.FILLER_CATEGORIES
import com.xwab.app.composition.OfflineCatalog.Companion.TRACK
import com.xwab.app.di.AppGraph
import kotlin.time.Duration.Companion.minutes

/**
 * The entry chain end to end, from the app's own root: a Metro-collected installer draws the Browse
 * screen, its ViewModel reads the catalog, a tap goes out through the feature's callback to the
 * navigator, and the category's installer draws its screen with an assisted ViewModel for that id.
 *
 * The unit tests each stop at one link: `AppEntryCallbacksTest` calls providers directly,
 * `AppEntryProviderTest` resolves entries without drawing them, and `NavigationCompositionTest`
 * draws synthetic entries. Only here are the real ones drawn and used.
 *
 * Runs on an Android device and an iOS simulator; each platform passes its own production graph,
 * built dynamically with [OfflineCatalog] in place of the downloaded catalog.
 */
internal fun realEntriesOpenACategoryFromBrowse(createGraph: (OfflineCatalog) -> AppGraph) = runComposeUiTest(
    // Two waits of up to TIMEOUT_MS each, past Compose's one-minute default, which the slow CI
    // emulator already exceeded with lighter tests.
    testTimeout = 3.minutes,
) {
    val graph = createGraph(OfflineCatalog())
    val owner = runOnIdle { TestRootOwner() }
    setContent {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides owner,
            LocalLifecycleOwner provides owner,
            LocalSavedStateRegistryOwner provides owner,
            LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
        ) {
            App(graph.metroViewModelFactory, AppEntryGraphs)
        }
    }
    try {
        waitUntilAtLeastOneExists(hasText(CATEGORY), TIMEOUT_MS)
        onNode(hasScrollToIndexAction()).performScrollToIndex(FILLER_CATEGORIES)
        // Gone, not merely hidden: on iOS `assertIsNotDisplayed` throws for a node the lazy grid
        // already disposed, while Android reports it as not displayed.
        onNodeWithText(CATEGORY).assertDoesNotExist()
        // The production bus, entry decorator and feature receiver must agree on one bus and key.
        repeat(RESELECTIONS) { onNode(SELECTED_TAB).performClick() }
        waitForIdle()
        onNodeWithText(CATEGORY).assertIsDisplayed()
        onNodeWithText(CATEGORY).performClick()

        waitUntilAtLeastOneExists(hasText(TRACK), TIMEOUT_MS)
    } finally {
        runOnIdle { owner.close() }
    }
}

/**
 * The now-playing bar builds the real playback engine on the main thread on the first frame. The
 * device tests settled on 45 seconds for that on a 2-core CI emulator; ten was not always enough.
 */
private const val TIMEOUT_MS = 45_000L

/** Repeated taps on the selected tab at its root; every one asks the list to scroll to its start. */
private const val RESELECTIONS = 3

/**
 * The tab the app starts on, Browse, found as the one selected navigation tab. Its label is the
 * shell's own resource, which this module cannot read; the bar and the rail both mark their items
 * as tabs.
 */
private val SELECTED_TAB = isSelected() and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
