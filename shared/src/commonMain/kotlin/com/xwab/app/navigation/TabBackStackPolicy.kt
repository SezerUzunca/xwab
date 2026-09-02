package com.xwab.app.navigation

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

/**
 * The tab rules, independent of which tabs exist or how their back stacks are implemented.
 *
 * Free of any concrete `StackNavigation`/`ChildStack` so the rules — easy to get subtly wrong, a
 * tab pushed onto another tab's history, a root popped out from under `Children` — are checked by
 * [TabBackStackPolicyTest] with fake tabs instead of the app's real three.
 *
 * @param startTab the tab back falls through to, and the one the app exits from.
 * @param stackSize a tab's current back-stack depth, read fresh on every call.
 * @param pop pops the top of a tab's stack. Never called while [stackSize] reports 1.
 * @param clearSubStack pops everything above a tab's own root.
 */
internal class TabBackStackPolicy<T : Any>(
    private val startTab: T,
    private val stackSize: (T) -> Int,
    private val pop: (T) -> Unit,
    private val clearSubStack: (T) -> Unit,
) {
    private val _selectedTab = MutableValue(startTab)
    val selectedTab: Value<T> = _selectedTab

    /**
     * Resets the current tab when it is reselected, or switches to another top-level tab.
     *
     * A tab is never pushed onto another tab's stack. Doing that would put one tab's history on
     * top of another's, and backing out of it would land in the middle of the other tab.
     */
    fun selectTab(tab: T) {
        if (tab == _selectedTab.value) {
            clearSubStack(tab)
        } else {
            _selectedTab.value = tab
        }
    }

    /**
     * Pops within the selected tab, and falls through to the start tab once there is nothing left
     * to pop.
     *
     * A tab's root is never popped: an emptied stack has nothing for `Children` to render. From
     * the start tab's own root this does nothing at all, leaving the platform's own back behavior
     * (e.g. leaving the app) to take over.
     */
    fun goBack() {
        val tab = _selectedTab.value
        when {
            stackSize(tab) > 1 -> pop(tab)
            tab != startTab -> selectTab(startTab)
        }
    }
}
