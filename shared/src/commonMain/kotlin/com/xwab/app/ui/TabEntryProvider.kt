package com.xwab.app.ui

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import com.xwab.app.navigation.savedIdentity

/**
 * Assigns display and decorator identity before entries from different tabs are combined.
 *
 * @param backStack the tab's stack, read when an entry is built so [metadataProvider] also learns
 *   what lies beneath it: a pane's role can depend on what it was opened from.
 * @param metadataProvider display metadata for (tab, destination, the destinations beneath it).
 */
internal fun entryProviderForTab(
    tab: NavKey,
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    metadataProvider: (NavKey, NavKey, List<NavKey>) -> Map<String, Any>,
): (NavKey) -> NavEntry<NavKey> {
    val tabId = tab.savedIdentity()
    return { key ->
        val entry = entryProvider(key)
        val beneath = backStack.take(backStack.indexOf(key).coerceAtLeast(0))
        val identity = metadata {
            put(TabKey, tabId)
            put(DestinationKey, key)
        }
        // Navigation 3's default content key is `toString()` and the class name, which renaming a
        // route's class changes. The saved identity follows its @SerialName, as the back stack does.
        // A content key the feature chose itself is kept.
        val entryKey = if (entry.contentKey == NavEntry(key) {}.contentKey) key.savedIdentity() else entry.contentKey
        NavEntry(
            key = key,
            // A String is savable on both platforms. The length prefix separates the tab from
            // the entry's content key even if either contains a delimiter.
            contentKey = "${tabId.length}:$tabId$entryKey",
            metadata = entry.metadata + metadataProvider(tab, key, beneath) + identity,
        ) { entry.Content() }
    }
}
