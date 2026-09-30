package com.xwab.app.ui

import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey

/** The tab an entry belongs to, set by [entryProviderForTab]; transitions fade between tabs and slide within one. */
internal object TabKey : NavMetadataKey<String>

/** The pane a destination was opened from; the back arrow is hidden while that pane is visible beside it. */
internal object ParentPaneKey : NavMetadataKey<ThreePaneScaffoldRole>

/**
 * The destination an entry draws. `NavEntry` keeps its key private, and a decorator sees only the
 * entry: [rememberAdaptiveBackNavEntryDecorator] reads this to close that destination's pane.
 */
internal object DestinationKey : NavMetadataKey<NavKey>
