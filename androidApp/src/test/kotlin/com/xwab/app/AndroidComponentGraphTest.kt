package com.xwab.app

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.xwab.app.di.AppGraph
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MetroX builds an Activity only if the graph the application hands it holds one; for any other
 * it falls back to AndroidX's reflective constructor, which `MainActivity` does not have. So a
 * contribution that missed this graph would build and lint cleanly and crash on launch.
 *
 * Building the graph creates no Activity, so this needs neither a device nor the real application
 * graph, whose own checks run on a device in `:composition`.
 */
class AndroidComponentGraphTest {
    @Test
    fun theLauncherActivityIsBuiltFromTheComponentGraph() {
        val graph = createComponentGraph(EmptyAppGraph)

        assertEquals(setOf<KClass<out Activity>>(MainActivity::class), graph.activityProviders.keys)
    }
}

private object EmptyAppGraph : AppGraph {
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel> = emptyMap()
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory> =
        emptyMap()
    override val manualAssistedFactoryProviders:
        Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory> = emptyMap()
    override val metroViewModelFactory: MetroViewModelFactory = object : MetroViewModelFactory() {}
}
