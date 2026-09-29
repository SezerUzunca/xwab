package com.xwab.app.ui

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata

/**
 * Assigns display and decorator identity before entries from different tabs are combined.
 *
 * @param onUp closes the given destination and what was opened from it; used by the back arrow
 *   of a pane shown beside others, see [AdaptiveBackControl].
 */
internal fun entryProviderForTab(
    tab: NavKey,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    metadataProvider: (NavKey, NavKey) -> Map<String, Any>,
    onUp: (NavKey) -> Unit,
): (NavKey) -> NavEntry<NavKey> {
    val tabId = tab.toString()
    return { key ->
        val entry = entryProvider(key)
        val displayMetadata = entry.metadata + metadataProvider(tab, key) + metadata { put(TabKey, tabId) }
        NavEntry(
            key = key,
            // A String is savable on both platforms. The length prefix separates the tab from
            // the feature's content key even if either contains a delimiter.
            contentKey = "${tabId.length}:$tabId${entry.contentKey}",
            metadata = displayMetadata,
        ) {
            AdaptiveBackControl(displayMetadata, onUp = { onUp(key) }) { entry.Content() }
        }
    }
}
