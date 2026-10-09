package com.xwab.app.composition

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider

/**
 * Installs the feature entries [entries] collected, wired to that host's navigator.
 *
 * Features receive their own callback contracts. Their ViewModels resolve from the factory in
 * composition only when an entry is drawn, so collecting installers creates no ViewModel or port.
 */
internal fun appEntryProvider(entries: AppEntries): (NavKey) -> NavEntry<NavKey> = entryProvider {
    entries.entryProviderInstallers.forEach { install -> install() }
}
