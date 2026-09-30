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
     * An entry's content key derives from its key by default, and a stack's saveable state holder
     * rejects a repeated content key. Popping to the existing entry is this app's answer; moving it
     * to the top, or giving every push its own content key, would be others.
     *
     * Anything else is pushed, as in the official recipes. Choosing Ocean after Rain beside a list
     * keeps Rain in history; Material's list–detail scene skips both on Back, because they share a
     * pane (`PopUntilCurrentDestinationChange`, set where the scene strategy is created).
     */
    fun navigate(key: NavKey) {
        if (key in state.backStacks) {
            state.topLevelRoute = key
            return
        }
        val stack = state.currentBackStack
        val existing = stack.indexOf(key)
        if (existing >= 0) stack.subList(existing + 1, stack.size).clear() else stack.add(key)
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
     * Opens [key] in place of a transient screen (the player), as a fresh selection from the tab's
     * root.
     *
     * What was open beneath the player is not the new destination's parent: a sound opened from
     * the player must not sit beside a category it does not belong to. A destination already on
     * the stack is popped to instead, and keeps the context it was opened in. Both changes land
     * before the next frame, so no intermediate destination is ever drawn.
     */
    fun replaceCurrent(key: NavKey) {
        val stack = state.currentBackStack
        if (key !in stack && key !in state.backStacks && stack.size > 1) stack.subList(1, stack.size).clear()
        navigate(key)
    }

    /**
     * Up from a destination visible beside others: closes its pane and whatever was opened from it.
     *
     * Back leaves the rightmost pane of a list–detail–extra scene; an arrow drawn on the middle
     * pane means that pane. Earlier selections directly beneath it, of the same kind, held the same
     * pane and go too, as Back would skip them. A destination not in the current stack, or its
     * root, falls back to [goBack].
     */
    fun goUp(from: NavKey) {
        val stack = state.currentBackStack
        var index = stack.indexOf(from)
        while (index > 1 && stack[index - 1]::class == from::class) index--
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
