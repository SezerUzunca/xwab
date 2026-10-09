package com.xwab.app.feature.category.navigation

import com.xwab.app.core.sound.port.TrackId

/** Actions supplied by the application's composition root. */
class CategoryEntryCallbacks(
    val onTrackClick: (TrackId) -> Unit,
    val onBack: () -> Unit,
)
