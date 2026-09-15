package com.xwab.app.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/** Assigns display and decorator identity before entries from different tabs are combined. */
internal fun entryProviderForTab(
    tab: NavKey,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): (NavKey) -> NavEntry<NavKey> {
    val tabId = tab.toString()
    return { key ->
        val entry = entryProvider(key)
        NavEntry(
            key = key,
            // A String is saveable on both platforms. The length prefix separates the tab from
            // the feature's content key even if either contains a delimiter.
            contentKey = "${tabId.length}:$tabId${entry.contentKey}",
            metadata = entry.metadata,
        ) {
            entry.Content()
        }
    }
}
