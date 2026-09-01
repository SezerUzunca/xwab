package com.xwab.app.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * A [CoroutineScope] tied to this component's lifecycle — every feature component's replacement
 * for `viewModelScope`.
 *
 * It is cancelled when the component is actually destroyed (popped off its stack or the app
 * process ends), not on every Android configuration change: the whole component tree hangs off a
 * single root retained via `retainedComponent`, so a component surviving rotation keeps the same
 * [InstanceKeeper] entry rather than recreating it.
 */
fun ComponentContext.componentScope(): CoroutineScope =
    instanceKeeper.getOrCreate(key = ComponentScopeKey) { ComponentCoroutineScope() }.scope

private object ComponentScopeKey

private class ComponentCoroutineScope : InstanceKeeper.Instance {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        scope.cancel()
    }
}
