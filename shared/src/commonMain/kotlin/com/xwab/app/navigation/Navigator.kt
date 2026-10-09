package com.xwab.app.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEventBus
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * App policies around the Navigation 3 stacks; features receive only intent callbacks.
 *
 * Built by the host's entry graph over the stacks the host restored, one per graph. Its result bus
 * is also provided to the entries: a root reselection sends that tab's route as a scroll signal.
 */
@Inject
@SingleIn(EntryProviderScope::class)
internal class Navigator(
    private val state: NavigationState,
    private val resultEventBus: ResultEventBus,
) {
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
            if (key != state.topLevelRoute) clearReselection()
            state.topLevelRoute = key
            return
        }
        val stack = state.currentBackStack
        val existing = stack.indexOf(key)
        if (existing != stack.lastIndex) clearReselection()
        if (existing >= 0) stack.subList(existing + 1, stack.size).clear() else stack.add(key)
    }

    /** Reselect returns to the root; another tap at the root asks the list to scroll to the start. */
    fun selectTab(key: NavKey) {
        require(key in state.backStacks) { "A tab needs its own back stack: $key" }
        if (key != state.topLevelRoute) navigate(key)
        else {
            val stack = state.currentBackStack
            if (stack.size > 1) stack.subList(1, stack.size).clear()
            // Use the concrete tab type: sendResult(key) would use NavKey as the result type.
            else resultEventBus.sendResult(key::class.toString(), key)
        }
    }

    /**
     * Opens [key] as a fresh selection from the current tab's root.
     *
     * What was open is not the new destination's parent: a sound opened from the now-playing bar
     * must not sit beside a category it does not belong to. A destination already on the stack is
     * popped to instead, and keeps the context it was opened in. Both changes land before the next
     * frame, so no intermediate destination is ever drawn.
     */
    fun replaceCurrent(key: NavKey) {
        val stack = state.currentBackStack
        if (key !in stack && key !in state.backStacks && stack.size > 1) stack.subList(1, stack.size).clear()
        navigate(key)
    }

    /**
     * Opens [key] in [tab], the tab it belongs to, as [replaceCurrent] would there.
     *
     * The now-playing bar's way to an item: a sound opens under Sounds and a story under Stories,
     * whichever tab the listener is on. Any other tab keeps its stack exactly as it was.
     */
    fun openInTab(tab: NavKey, key: NavKey) {
        require(tab in state.backStacks) { "A tab needs its own back stack: $tab" }
        navigate(tab)
        replaceCurrent(key)
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
        clearReselection()
        val stack = state.currentBackStack
        var index = stack.indexOf(from)
        while (index > 1 && stack[index - 1]::class == from::class) index--
        if (index > 0) stack.subList(index, stack.size).clear() else goBack()
    }

    fun goBack() {
        clearReselection()
        val stack = state.currentBackStack
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            state.topLevelRoute != state.startRoute -> state.topLevelRoute = state.startRoute
        }
    }

    /**
     * A later visit must not replay a scroll request queued before leaving this destination.
     *
     * Clearing closes the key's channel. A receiver still composed, such as a list beside a detail
     * pane, moves to the next one, as the documented clear-inside-`ResultEffect` pattern relies on.
     */
    private fun clearReselection() = resultEventBus.removeResult(state.topLevelRoute::class.toString())
}
