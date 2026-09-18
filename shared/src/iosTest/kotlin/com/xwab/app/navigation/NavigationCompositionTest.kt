@file:OptIn(
    androidx.compose.ui.test.ExperimentalTestApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
)

package com.xwab.app.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.xwab.app.composition.NowPlayingSceneDecoratorStrategy
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the production navigation state, entry decorators and scene decorator in a composition.
 * Plain Navigator tests cannot detect a ViewModel store collision or a lost rememberSaveable value.
 * The simulator is the existing project harness for common Compose code; no platform app is needed.
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
            }
            onNodeWithText(restoredFavoritesModel.label(2)).assertExists()

            navigate(harness, BrowseRoute)
            val restoredBrowseModel = runOnIdle { harness.lastModel(sound) }
            assertNotSame(oldBrowseModel, restoredBrowseModel)
            assertNotSame(restoredFavoritesModel, restoredBrowseModel)
            onNodeWithText(restoredBrowseModel.label(1)).assertExists()
            back(harness)
            runOnIdle { assertEquals(category, harness.state.currentBackStack.last()) }
        }
    }

    @Test
    fun sceneChromeUsesOneRootViewModelWhileEntriesUseChildStores() = runComposeUiTest {
        withNavigation { harness ->
            val sound = SoundRoute("rain")
            navigate(harness, sound)
            back(harness)
            navigate(harness, FavoritesRoute)

            runOnIdle {
                assertTrue(harness.chromeOwners.isNotEmpty())
                harness.chromeOwners.forEach { assertSame(harness.rootOwner, it) }
                assertEquals(1, harness.chromeModels.size, "both transition scenes must share chrome")
                assertFalse(harness.chromeModels.single().cleared)
                assertTrue(harness.models.first { it.route == sound }.cleared)
                harness.entryOwners.forEach { assertNotSame(harness.rootOwner, it) }
            }
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
    runOnIdle { Navigator(harness.state).navigate(route) }
    waitForIdle()
}

private fun ComposeUiTest.back(harness: NavigationHarness) {
    runOnIdle { Navigator(harness.state).goBack() }
    waitForIdle()
}

private class NavigationHarness {
    var visible by mutableStateOf(true)
    var rootOwner = TestRootOwner()
        private set
    var registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        private set
    lateinit var state: NavigationState
        private set
    val models = mutableListOf<EntryViewModel>()
    val entryOwners = mutableSetOf<ViewModelStoreOwner>()
    val chromeOwners = mutableSetOf<ViewModelStoreOwner>()
    val chromeModels = mutableSetOf<ChromeViewModel>()

    fun lastModel(route: NavKey) = models.last { it.route == route }

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
        CompositionLocalProvider(
            LocalSaveableStateRegistry provides registry,
            LocalViewModelStoreOwner provides rootOwner,
            LocalLifecycleOwner provides rootOwner,
            LocalSavedStateRegistryOwner provides rootOwner,
            LocalNavigationEventDispatcherOwner provides dispatcher,
        ) {
            val navigationState = rememberNavigationState()
            val navigator = remember(navigationState) { Navigator(navigationState) }
            val provider: (NavKey) -> NavEntry<NavKey> = remember {
                { route -> NavEntry(route) { Entry(route) } }
            }
            SideEffect { state = navigationState }
            SharedTransitionLayout {
                val transitionScope = this
                val decorator = remember(transitionScope) {
                    NowPlayingSceneDecoratorStrategy<NavKey>(transitionScope) { Chrome() }
                }
                NavDisplay(
                    entries = navigationState.toEntries(provider),
                    sceneDecoratorStrategies = listOf(decorator),
                    sharedTransitionScope = transitionScope,
                    onBack = navigator::goBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Composable
    private fun Entry(route: NavKey) {
        val owner = checkNotNull(LocalViewModelStoreOwner.current)
        val model = viewModel { EntryViewModel(route, models.size).also { models.add(it) } }
        var savedCount by rememberSaveable { mutableIntStateOf(0) }
        SideEffect { entryOwners += owner }
        BasicText(model.label(savedCount), Modifier.clickable { savedCount++ })
    }

    @Composable
    private fun Chrome() {
        val owner = checkNotNull(LocalViewModelStoreOwner.current)
        val model = viewModel { ChromeViewModel() }
        SideEffect {
            chromeOwners += owner
            chromeModels += model
        }
        BasicText("Persistent chrome")
    }
}

private class EntryViewModel(val route: NavKey, private val id: Int) : ViewModel() {
    var cleared = false
        private set

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

private class TestRootOwner : ViewModelStoreOwner, SavedStateRegistryOwner {
    override val viewModelStore = ViewModelStore()
    override val lifecycle = LifecycleRegistry.createUnsafe(this)
    private val controller = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performAttach()
        controller.performRestore(null)
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    fun close() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
