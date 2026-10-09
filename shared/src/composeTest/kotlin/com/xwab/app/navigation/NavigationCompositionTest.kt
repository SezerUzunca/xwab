@file:OptIn(
    androidx.compose.ui.test.ExperimentalTestApi::class,
)

package com.xwab.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.composition.appEntryGraph
import com.xwab.app.composition.appEntryMetadata
import com.xwab.app.TestRootOwner
import com.xwab.app.designsystem.components.LocalBackButtonVisibility
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.ui.AppNavigationDisplay
import com.xwab.app.ui.rememberTabEntries
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the production navigation state, entry decorators, navigation suite and now-playing bar slot.
 * Plain Navigator tests cannot detect a ViewModel store collision or a lost rememberSaveable value.
 * Entry ViewModels are looked up through MetroX, as production screens look theirs up, and keep
 * state in their SavedStateHandle; it survives recreation in its own entry only while the
 * decorators run in their production order.
 * The now-playing bar slot sits outside `NavDisplay`, so it is drawn once and keeps the root ViewModel owner
 * whatever the scenes do. Android device tests and iOS simulator tests share this suite.
 */
class NavigationCompositionTest {
    @Test
    fun theSameRouteInTwoTabsHasIndependentStoresThatSurviveSwitchesAndClearOnPop() = runComposeUiTest {
        withNavigation { harness ->
            val sound = SoundRoute("rain")
            navigate(harness, sound)
            val browseModel = runOnIdle { harness.lastModel(sound) }

            navigate(harness, FavoritesRoute)
            navigate(harness, sound)
            val favoritesModel = runOnIdle { harness.lastModel(sound) }
            assertNotSame(browseModel, favoritesModel)

            navigate(harness, BrowseRoute)
            onNodeWithText(browseModel.label(0)).assertExists()
            runOnIdle {
                assertFalse(browseModel.cleared)
                assertFalse(favoritesModel.cleared)
                assertEquals(2, harness.models.count { it.route == sound })
            }

            navigate(harness, FavoritesRoute)
            onNodeWithText(favoritesModel.label(0)).assertExists()
            back(harness)
            runOnIdle {
                assertTrue(favoritesModel.cleared, "pop must clear only the selected tab's entry")
                assertFalse(browseModel.cleared)
            }

            back(harness) // Favorites root falls through to the preserved Browse stack.
            onNodeWithText(browseModel.label(0)).assertExists()
            back(harness)
            runOnIdle { assertTrue(browseModel.cleared) }
        }
    }

    @Test
    fun recreationRestoresSelectedTabInactiveStacksArgumentsAndEntrySaveableState() = runComposeUiTest {
        withNavigation { harness ->
            val category = CategoryRoute("rain / yağmur:夜")
            val sound = SoundRoute("rain|night/%25")
            navigate(harness, category)
            navigate(harness, sound)
            val oldBrowseModel = runOnIdle { harness.lastModel(sound) }
            onNodeWithText(oldBrowseModel.label(0)).performClick()

            navigate(harness, FavoritesRoute)
            navigate(harness, sound)
            val oldFavoritesModel = runOnIdle { harness.lastModel(sound) }
            onNodeWithText(oldFavoritesModel.label(0)).performClick()
            onNodeWithText(oldFavoritesModel.label(1)).performClick()
            val expectedStacks = runOnIdle { harness.state.backStacks.mapValues { it.value.toList() } }

            // Discard the whole subtree and its ViewModelStore, not just recomposing with the same
            // objects. The new tree gets only the registry payload that rememberSaveable wrote.
            val saved = runOnIdle {
                harness.registry.performSave().also { harness.visible = false }
            }
            waitForIdle()
            runOnIdle { harness.restoreInNewRoot(saved) }
            waitForIdle()

            val restoredFavoritesModel = runOnIdle { harness.lastModel(sound) }
            runOnIdle {
                assertTrue(oldBrowseModel.cleared)
                assertTrue(oldFavoritesModel.cleared)
                assertEquals(FavoritesRoute, harness.state.topLevelRoute)
                assertEquals(expectedStacks, harness.state.backStacks.mapValues { it.value.toList() })
                assertNotSame(oldFavoritesModel, restoredFavoritesModel)
                // A ViewModel's SavedStateHandle is saved with its own entry, not with the root.
                assertEquals(2, restoredFavoritesModel.taps)
            }
            onNodeWithText(restoredFavoritesModel.label(2)).assertExists()

            navigate(harness, BrowseRoute)
            val restoredBrowseModel = runOnIdle { harness.lastModel(sound) }
            assertNotSame(oldBrowseModel, restoredBrowseModel)
            assertNotSame(restoredFavoritesModel, restoredBrowseModel)
            onNodeWithText(restoredBrowseModel.label(1)).assertExists()
            runOnIdle {
                assertEquals(1, restoredBrowseModel.taps, "the same route in another tab keeps its own handle")
            }
            back(harness)
            runOnIdle { assertEquals(category, harness.state.currentBackStack.last()) }
        }
    }

    @Test
    fun appChromeUsesOneRootViewModelWhileEntriesUseChildStores() = runComposeUiTest {
        withNavigation { harness ->
            val sound = SoundRoute("rain")
            navigate(harness, sound)
            back(harness)
            navigate(harness, FavoritesRoute)

            runOnIdle {
                assertTrue(harness.chromeOwners.isNotEmpty())
                harness.chromeOwners.forEach { assertSame(harness.rootOwner, it) }
                assertEquals(1, harness.chromeModels.size, "navigation must retain the app's chrome")
                assertFalse(harness.chromeModels.single().cleared)
                assertTrue(harness.models.first { it.route == sound }.cleared)
                harness.entryOwners.forEach { assertNotSame(harness.rootOwner, it) }
            }
        }
    }

    @Test
    fun changingBetweenCompactAndAdaptiveScenesRetainsStoresStateAndOneChrome() = runComposeUiTest {
        withNavigation { harness ->
            val category = CategoryRoute("rain")
            val sound = SoundRoute("rain")
            navigate(harness, category)
            val categoryModel = runOnIdle { harness.lastModel(category) }
            navigate(harness, sound)
            val soundModel = runOnIdle { harness.lastModel(sound) }
            onNodeWithText(soundModel.label(0)).performClick()

            runOnIdle { harness.wide = true }
            waitForIdle()
            onNodeWithText(categoryModel.label(0)).assertExists()
            onNodeWithText(soundModel.label(1)).assertExists()
            onNodeWithText("Up:$sound").assertDoesNotExist()
            onAllNodesWithText("Persistent chrome").assertCountEquals(1)

            runOnIdle { harness.wide = false }
            waitForIdle()
            onNodeWithText(soundModel.label(1)).assertExists()
            onNodeWithText("Up:$sound").assertExists()
            runOnIdle {
                assertSame(soundModel, harness.lastModel(sound))
                assertFalse(categoryModel.cleared)
                assertEquals(1, harness.chromeModels.size)
            }
        }
    }

    @Test
    fun rootReselectionsReachOnlyTheirOwnEntriesThroughTheGraphBus() = runComposeUiTest {
        withNavigation { harness ->
            runOnIdle {
                harness.navigator.navigate(CategoryRoute("rain"))
                harness.navigator.selectTab(BrowseRoute)
            }
            waitForIdle()
            runOnIdle { assertTrue(harness.reselections.isEmpty(), "returning to the root is not a reselection") }

            // Typed explicitly: Kotlin/Native cannot infer assertEquals' T from the routes' intersection type.
            val tabs = listOf<NavKey>(BrowseRoute, FavoritesRoute, StoriesRoute)
            tabs.forEachIndexed { index, tab ->
                runOnIdle {
                    if (harness.state.topLevelRoute != tab) harness.navigator.selectTab(tab)
                }
                waitForIdle()
                runOnIdle {
                    assertEquals(tabs.take(index), harness.reselections, "switching tabs must not send a result")
                    harness.navigator.selectTab(tab)
                }
                waitForIdle()
                runOnIdle { assertEquals(tabs.take(index + 1), harness.reselections) }
            }
        }
    }

    @Test
    fun leavingTheRootDiscardsAReselectionQueuedBeforeItsReceiverWasReady() = runComposeUiTest {
        withNavigation { harness ->
            runOnIdle { harness.receiveReselections = false }
            waitForIdle()
            runOnIdle {
                harness.navigator.selectTab(BrowseRoute)
                harness.navigator.navigate(CategoryRoute("rain"))
            }
            waitForIdle()
            runOnIdle {
                harness.navigator.goBack()
                harness.receiveReselections = true
            }
            waitForIdle()
            runOnIdle {
                assertTrue(harness.reselections.isEmpty(), "a later visit must not replay an old scroll request")
                harness.navigator.selectTab(BrowseRoute)
            }
            waitForIdle()
            runOnIdle { assertEquals(listOf<NavKey>(BrowseRoute), harness.reselections) }
        }
    }

    /**
     * Clearing closes the channel a composed receiver is collecting; it must move to the new one.
     * Back at the start tab's root changes nothing on screen but still clears the root's key.
     */
    @Test
    fun aReceiverThatStaysComposedStillHearsReselectionsAfterAClear() = runComposeUiTest {
        withNavigation { harness ->
            val starts = runOnIdle { harness.receiverStarts.getValue(BrowseRoute) }
            back(harness)
            runOnIdle {
                assertEquals(listOf<NavKey>(BrowseRoute), harness.currentStack())
                harness.navigator.selectTab(BrowseRoute)
            }
            waitForIdle()
            runOnIdle {
                assertEquals(listOf<NavKey>(BrowseRoute), harness.reselections)
                assertEquals(starts, harness.receiverStarts.getValue(BrowseRoute), "the receiver was recreated")
            }
        }
    }

    /** Beside a detail pane the list stays composed through both clears: opening and closing it. */
    @Test
    fun aListBesideADetailPaneStillHearsReselectionsOnceThePaneCloses() = runComposeUiTest {
        withNavigation { harness ->
            runOnIdle { harness.wide = true }
            waitForIdle()
            val starts = runOnIdle { harness.receiverStarts.getValue(BrowseRoute) }
            navigate(harness, CategoryRoute("rain"))
            systemBack(harness)
            runOnIdle {
                assertEquals(listOf<NavKey>(BrowseRoute), harness.currentStack())
                harness.navigator.selectTab(BrowseRoute)
            }
            waitForIdle()
            runOnIdle {
                assertEquals(listOf<NavKey>(BrowseRoute), harness.reselections)
                assertEquals(starts, harness.receiverStarts.getValue(BrowseRoute), "the list left composition")
            }
        }
    }

    /** Platform Back, completed or as a predictive gesture, reaches `NavDisplay` and pops one screen. */
    @Test
    fun systemBackPopsTheLatestScreenAndFallsThroughToTheStartTab() = runComposeUiTest {
        withNavigation { harness ->
            navigate(harness, CategoryRoute("rain"))
            navigate(harness, SoundRoute("rain"))

            systemBack(harness)
            runOnIdle { assertEquals(listOf<NavKey>(BrowseRoute, CategoryRoute("rain")), harness.currentStack()) }

            systemBack(harness, predictive = true)
            runOnIdle { assertEquals(listOf<NavKey>(BrowseRoute), harness.currentStack()) }

            navigate(harness, FavoritesRoute)
            systemBack(harness)
            runOnIdle { assertEquals(BrowseRoute, harness.state.topLevelRoute) }
        }
    }

    /**
     * Beside the list, one platform Back leaves the detail pane and every earlier selection in it:
     * Material's scene asks `NavDisplay` for as many pops as `PopUntilCurrentDestinationChange` skips.
     */
    @Test
    fun systemBackLeavesAPaneInOneStepBesideTheList() = runComposeUiTest {
        withNavigation { harness ->
            runOnIdle { harness.wide = true }
            waitForIdle()
            navigate(harness, CategoryRoute("rain"))
            navigate(harness, CategoryRoute("ocean"))

            systemBack(harness)

            runOnIdle { assertEquals(listOf<NavKey>(BrowseRoute), harness.currentStack()) }
        }
    }
}

private fun ComposeUiTest.withNavigation(block: ComposeUiTest.(NavigationHarness) -> Unit) {
    val harness = runOnIdle { NavigationHarness() }
    setContent { harness.Content() }
    waitForIdle()
    try {
        block(harness)
    } finally {
        runOnIdle { harness.visible = false }
        waitForIdle()
        runOnIdle { harness.rootOwner.close() }
    }
}

private fun ComposeUiTest.navigate(harness: NavigationHarness, route: NavKey) {
    runOnIdle { harness.navigator.navigate(route) }
    waitForIdle()
}

private fun ComposeUiTest.back(harness: NavigationHarness) {
    runOnIdle { harness.navigator.goBack() }
    waitForIdle()
}

/** Back as the platform delivers it: through the dispatcher `NavDisplay` listens to. */
private fun ComposeUiTest.systemBack(harness: NavigationHarness, predictive: Boolean = false) {
    runOnIdle {
        val dispatcher = harness.navigationEvents.navigationEventDispatcher
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        if (predictive) {
            input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
            input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.5f))
        }
        input.backCompleted()
        dispatcher.removeInput(input)
    }
    waitForIdle()
}

private class NavigationHarness {
    var visible by mutableStateOf(true)
    var wide by mutableStateOf(false)
    var rootOwner = TestRootOwner()
        private set
    var registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        private set
    lateinit var state: NavigationState
        private set
    lateinit var navigator: Navigator
        private set
    var receiveReselections by mutableStateOf(true)
    val reselections = mutableListOf<NavKey>()

    /** How often each root's receiver entered composition: unchanged means it stayed composed. */
    val receiverStarts = mutableMapOf<NavKey, Int>()
    lateinit var navigationEvents: NavigationEventDispatcherOwner
        private set
    val models = mutableListOf<EntryViewModel>()

    fun currentStack(): List<NavKey> = state.currentBackStack.toList()
    val entryOwners = mutableSetOf<ViewModelStoreOwner>()
    val chromeOwners = mutableSetOf<ViewModelStoreOwner>()
    val chromeModels = mutableSetOf<ChromeViewModel>()

    fun lastModel(route: NavKey) = models.last { it.route == route }

    /** What the app graph contributes in production, reduced to this suite's two ViewModels. */
    private val viewModelFactory = object : MetroViewModelFactory() {
        override val viewModelProviders = mapOf<KClass<out ViewModel>, () -> ViewModel>(
            ChromeViewModel::class to { ChromeViewModel() },
        )
        override val manualAssistedFactoryProviders =
            mapOf<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>(
                EntryViewModel.Factory::class to {
                    EntryViewModel.Factory { route, handle ->
                        EntryViewModel(route, models.size, handle).also { models.add(it) }
                    }
                },
            )
    }

    fun restoreInNewRoot(saved: Map<String, List<Any?>>) {
        rootOwner.close()
        rootOwner = TestRootOwner()
        registry = SaveableStateRegistry(restoredValues = saved, canBeSaved = { true })
        visible = true
    }

    @Composable
    fun Content() {
        if (!visible) return
        val dispatcher = rememberNavigationEventDispatcherOwner(parent = null)
        SideEffect { navigationEvents = dispatcher }
        val platformDensity = LocalDensity.current
        // The window size class is read in dp, so the density sets it. Platform defaults differ: an
        // iOS test window is 1024 px at density 1, already wide, while a phone is compact.
        val windowWidthPx = LocalWindowInfo.current.containerSize.width
        val widthDp = if (wide) WIDE_WIDTH_DP else COMPACT_WIDTH_DP
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides viewModelFactory,
            LocalSaveableStateRegistry provides registry,
            LocalViewModelStoreOwner provides rootOwner,
            LocalLifecycleOwner provides rootOwner,
            LocalSavedStateRegistryOwner provides rootOwner,
            LocalNavigationEventDispatcherOwner provides dispatcher,
            LocalDensity provides if (windowWidthPx > 0) {
                Density(windowWidthPx / widthDp, platformDensity.fontScale)
            } else {
                platformDensity
            },
        ) {
            SleepRelaxTheme { NavigationContent() }
        }
    }

    @Composable
    private fun NavigationContent() {
        val navigationState = rememberNavigationState()
        val graph = remember(navigationState) { appEntryGraph(navigationState) }
        val navigator = graph.navigator
        val provider: (NavKey) -> NavEntry<NavKey> = remember {
            { route -> NavEntry(route) { Entry(route) } }
        }
        SideEffect {
            state = navigationState
            this.navigator = navigator
        }
        AppNavigationDisplay(
            entries = rememberTabEntries(
                navigationState,
                provider,
                ::appEntryMetadata,
                navigator::goUp,
                graph.resultEventBus,
            ),
            selectedTab = navigationState.topLevelRoute,
            onSelectTab = navigator::selectTab,
            onBack = navigator::goBack,
            nowPlayingBar = { Chrome() },
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun Entry(route: NavKey) {
        if (receiveReselections) {
            DisposableEffect(route) {
                receiverStarts[route] = (receiverStarts[route] ?: 0) + 1
                onDispose {}
            }
            when (route) {
                BrowseRoute -> ResultEffect<BrowseRoute> { reselections += it }
                FavoritesRoute -> ResultEffect<FavoritesRoute> { reselections += it }
                StoriesRoute -> ResultEffect<StoriesRoute> { reselections += it }
            }
        }
        val owner = checkNotNull(LocalViewModelStoreOwner.current)
        // The lookup production entries make, taking the entry's saved state the way a screen that
        // needs one would: from the extras MetroX passes to the lambda.
        val model = assistedMetroViewModel<EntryViewModel, EntryViewModel.Factory> { extras ->
            create(route, extras.createSavedStateHandle())
        }
        var savedCount by rememberSaveable { mutableIntStateOf(0) }
        SideEffect { entryOwners += owner }
        Column {
            BasicText(
                model.label(savedCount),
                Modifier.clickable {
                    savedCount++
                    model.tap()
                },
            )
            if (route !in TOP_LEVEL_DESTINATIONS.map { it.route } && LocalBackButtonVisibility.current) {
                BasicText("Up:$route")
            }
        }
    }

    @Composable
    private fun Chrome() {
        val owner = checkNotNull(LocalViewModelStoreOwner.current)
        val model: ChromeViewModel = metroViewModel()
        SideEffect {
            chromeOwners += owner
            chromeModels += model
        }
        BasicText("Persistent chrome")
    }
}

private class EntryViewModel(
    val route: NavKey,
    private val id: Int,
    private val handle: SavedStateHandle,
) : ViewModel() {
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(route: NavKey, handle: SavedStateHandle): EntryViewModel
    }

    var cleared = false
        private set

    /** Taps kept in this entry's [SavedStateHandle], as a screen's own ViewModel would keep state. */
    val taps: Int
        get() = handle[TAPS_KEY] ?: 0

    fun tap() {
        handle[TAPS_KEY] = taps + 1
    }

    fun label(savedCount: Int) = "entry:$id saved:$savedCount"

    override fun onCleared() {
        cleared = true
    }
}

private class ChromeViewModel : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

private const val TAPS_KEY = "taps"

/** Compact and expanded width classes, the latter with room for two panes. */
private const val COMPACT_WIDTH_DP = 400f
private const val WIDE_WIDTH_DP = 1000f
