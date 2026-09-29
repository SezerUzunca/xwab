package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** App policies around the Navigation 3 stacks; features receive only intent callbacks. */
internal class Navigator(private val state: NavigationState) {
    private val reselectEvents = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)

    fun reselections(route: NavKey): Flow<Unit> = reselectEvents.filter { it == route }.map { }

    /**
     * Switching tabs preserves history. Revisiting a key pops to it, preserving its entry store.
     *
     * Opening another destination of a kind already in the stack replaces that one and whatever
     * was opened from it: choosing Ocean after Rain is a new selection, not a step deeper. Beside a
     * list that is what a list–detail pane shows, and Back then leaves the selection instead of
     * walking through every earlier one.
     */
    fun navigate(key: NavKey) {
        if (key in state.backStacks) {
            state.topLevelRoute = key
            return
        }
        val stack = state.currentBackStack
        val existing = stack.indexOf(key)
        if (existing >= 0) {
            stack.subList(existing + 1, stack.size).clear()
            return
        }
        val sameKind = stack.indexOfFirst { it::class == key::class }
        if (sameKind > 0) stack.subList(sameKind, stack.size).clear()
        stack.add(key)
    }

    /** Reselect returns to the root; another tap at the root asks the list to scroll to the start. */
    fun selectTab(key: NavKey) {
        require(key in state.backStacks) { "A tab needs its own back stack: $key" }
        if (key != state.topLevelRoute) navigate(key)
        else {
            val stack = state.currentBackStack
            if (stack.size > 1) stack.subList(1, stack.size).clear()
            else reselectEvents.tryEmit(key)
        }
    }

    /**
     * Replaces a transient screen (the player). Both changes land before the next frame, so no
     * intermediate destination is ever drawn.
     */
    fun replaceCurrent(key: NavKey) {
        val stack = state.currentBackStack
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        navigate(key)
    }

    /**
     * Up from a destination visible beside others: closes it and whatever was opened from it.
     *
     * Back removes only the latest entry, which in a list–detail–extra scene is the rightmost
     * pane. An arrow drawn on the middle pane means that pane. A destination not in the current
     * stack, or its root, falls back to [goBack].
     */
    fun goUp(from: NavKey) {
        val stack = state.currentBackStack
        val index = stack.indexOf(from)
        if (index > 0) stack.subList(index, stack.size).clear() else goBack()
    }

    fun goBack() {
        val stack = state.currentBackStack
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            state.topLevelRoute != state.startRoute -> state.topLevelRoute = state.startRoute
        }
    }
}
