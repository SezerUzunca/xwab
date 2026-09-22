package com.xwab.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.allStringResources

/**
 * The tab labels this module ships and the tabs that use them are the same set.
 *
 * The only thing in a feature's removal that the compiler has nothing to say about. Deleting
 * `:feature:story` breaks every other reference to it — the graph accessor, the entry provider, the
 * serializer list, the tab itself — but `tab_stories` keeps resolving happily to a string no screen
 * asks for, and ships in the APK forever.
 *
 * Checked in both directions on purpose. A label with no tab is the leftover; a tab with no label
 * cannot happen today, because [TopLevelDestination] takes a `StringResource` rather than a name,
 * and this states that it stays that way.
 *
 * `tab_` is the prefix, not the whole resource table: this module is free to own strings that are
 * not tab labels, and they are none of this test's business.
 */
class TopLevelDestinationsTest {

    @Test
    fun everyTabLabelBelongsToATabAndEveryTabHasOne() {
        val declared = Res.allStringResources.keys.filter { it.startsWith("tab_") }
        val used = TOP_LEVEL_DESTINATIONS.map { it.label.key }

        assertEquals(
            declared.sorted(),
            used.sorted(),
            "a tab label and the tabs disagree: an app.xml entry outlived the tab it named, " +
                "or a tab was added without one",
        )
    }

    /**
     * The first entry is the start destination — the tab the app opens on, falls back to on back,
     * and exits from. Reordering this list is a product decision, not a formatting one.
     */
    @Test
    fun browseIsTheStartDestination() {
        assertEquals("tab_browse", TOP_LEVEL_DESTINATIONS.first().label.key)
    }
}
