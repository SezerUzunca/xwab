package com.xwab.app.feature.browse.impl

import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.category
import com.xwab.app.core.ui.state.Loadable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun mapsCatalogCategoriesToReadyState() = runTest(mainDispatcher) {
        val categories = listOf(category("rain"), category("ocean"))
        val viewModel = BrowseViewModel(FakeMusicCatalog(categories = categories))
        collectState(viewModel)
        advanceUntilIdle()

        val state = assertIs<Loadable.Ready<BrowseState>>(viewModel.state.value).value
        assertEquals(categories, state.categories)
    }

    private fun TestScope.collectState(viewModel: BrowseViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }
}
