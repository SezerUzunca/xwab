@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.xwab.app.composition

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.xwab.app.composition.OfflineCatalog.Companion.NARRATOR
import com.xwab.app.composition.OfflineCatalog.Companion.STORY
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
    val owner = setApp(createGraph(OfflineCatalog()))
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
 * Every screen the app has, opened through its real entry on the production graph: the sound's own
 * screen, the Favorites tab, the Stories tab, a story's own screen, the Shuffle tab, the After
 * Shift tab and the two screens opened from it, for a nightmare and for lying awake, after Browse
 * and Category.
 *
 * Together with the bar on the root, that resolves all eleven contributed ViewModels — eight plain,
 * three through assisted factories with the id their route carries — rather than only checking the
 * maps are non-empty, as the graph test does. A screen whose ViewModel or installer never reached
 * the graph throws here when it opens.
 *
 * The expected texts are the screens' own English strings: their resources belong to the feature
 * modules, which this module installs but cannot read.
 */
internal fun realEntriesOpenEveryScreen(createGraph: (OfflineCatalog) -> AppGraph) = runComposeUiTest(
    // A wait of up to TIMEOUT_MS per screen; the first carries the engine's start on a slow emulator.
    testTimeout = 6.minutes,
) {
    val owner = setApp(createGraph(OfflineCatalog()))
    try {
        // A sound's own screen, behind Category: two assisted ViewModels, each given its route's id.
        waitUntilAtLeastOneExists(hasText(CATEGORY), TIMEOUT_MS)
        onNodeWithText(CATEGORY).performClick()
        waitUntilAtLeastOneExists(hasText(TRACK), TIMEOUT_MS)
        onNodeWithText(TRACK).performClick()
        // Only the sound's screen states its availability; the fake reports nothing cached.
        waitUntilAtLeastOneExists(hasText(SOUND_ONLINE_ONLY), TIMEOUT_MS)

        // The Favorites tab lists the favourite the fake starts with.
        onAllNodes(TAB)[FAVORITES_TAB].performClick()
        waitUntilAtLeastOneExists(hasText(FAVORITES_TITLE), TIMEOUT_MS)
        waitUntil(timeoutMillis = TIMEOUT_MS) { isDisplayed(TRACK) }

        // The Stories tab, then the story's own screen, which alone names its narrator.
        onAllNodes(TAB)[STORIES_TAB].performClick()
        waitUntilAtLeastOneExists(hasText(STORY), TIMEOUT_MS)
        onNodeWithText(STORY).performClick()
        waitUntilAtLeastOneExists(hasText("Narrated by $NARRATOR"), TIMEOUT_MS)

        // The Shuffle tab lists the shipped deck from the production catalog: nothing is spoken
        // until play is pressed, so there is nothing to replace.
        onAllNodes(TAB)[SHUFFLE_TAB].performClick()
        waitUntilAtLeastOneExists(hasText(SHUFFLE_TITLE), TIMEOUT_MS)
        waitUntilAtLeastOneExists(hasText(SHUFFLE_DECK), TIMEOUT_MS)

        // The After Shift tab: the production guides, and the note field over the production notes
        // store, which reading leaves unwritten.
        onAllNodes(TAB)[AFTER_SHIFT_TAB].performClick()
        waitUntilAtLeastOneExists(hasText(AFTER_SHIFT_NOTE_TITLE), TIMEOUT_MS)
        waitUntilAtLeastOneExists(hasText(AFTER_SHIFT_GUIDE), TIMEOUT_MS)

        // The nightmare screen, through the After Shift callback the shell supplies: its field over
        // the production notes store, which reading leaves unwritten.
        onNodeWithText(AFTER_SHIFT_REWRITE).performScrollTo().performClick()
        waitUntilAtLeastOneExists(hasText(REHEARSAL_FIELD_LABEL), TIMEOUT_MS)

        // Back to After Shift, then the screen for lying awake through the other callback: only the
        // guide the production catalog ships for out of bed.
        onNodeWithContentDescription(BACK).performClick()
        waitUntilAtLeastOneExists(hasText(AFTER_SHIFT_CANT_SLEEP), TIMEOUT_MS)
        onNodeWithText(AFTER_SHIFT_CANT_SLEEP).performScrollTo().performClick()
        waitUntilAtLeastOneExists(hasText(CANT_SLEEP_GUIDE), TIMEOUT_MS)
    } finally {
        runOnIdle { owner.close() }
    }
}

/** The app's own root on [graph], under one owner the scenario closes. */
private fun ComposeUiTest.setApp(graph: AppGraph): TestRootOwner {
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
    return owner
}

/** Whether any node with [text] is on screen; another tab may still hold one that is not. */
private fun ComposeUiTest.isDisplayed(text: String): Boolean {
    val nodes = onAllNodesWithText(text)
    return nodes.fetchSemanticsNodes().indices.any { index ->
        runCatching { nodes[index].assertIsDisplayed() }.isSuccess
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

/** Every navigation tab, in the shell's order: Browse, Favorites, Stories, Shuffle, After Shift. */
private val TAB = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
private const val FAVORITES_TAB = 1
private const val STORIES_TAB = 2
private const val SHUFFLE_TAB = 3
private const val AFTER_SHIFT_TAB = 4

/**
 * `feature:sound`'s `internet_required`, `feature:favorites`' `favorites_title`, `feature:shuffle`'s
 * `shuffle_title`, the title of the deck `core:shuffle` ships, `feature:after-shift`'s
 * `after_shift_note_title`, the first guide `core:guide` ships, `feature:after-shift`'s
 * `after_shift_nightmare_open`, `feature:rehearsal`'s `rehearsal_field_label`, `feature:after-shift`'s
 * `after_shift_cant_sleep_open` and the out-of-bed guide `core:guide` ships.
 */
private const val SOUND_ONLINE_ONLY = "Internet required"
private const val FAVORITES_TITLE = "Favorite sounds"
private const val SHUFFLE_TITLE = "Cognitive Shuffle"
private const val SHUFFLE_DECK = "Everyday Things"
private const val AFTER_SHIFT_NOTE_TITLE = "Park it for later"
private const val AFTER_SHIFT_GUIDE = "Muscle Relaxation"
private const val AFTER_SHIFT_REWRITE = "Rewrite a nightmare"
private const val REHEARSAL_FIELD_LABEL = "Your new dream"
private const val AFTER_SHIFT_CANT_SLEEP = "Can’t sleep"
private const val CANT_SLEEP_GUIDE = "Out of Bed"

/** `designsystem`'s `back`, the back arrow's content description. */
private const val BACK = "Back"
