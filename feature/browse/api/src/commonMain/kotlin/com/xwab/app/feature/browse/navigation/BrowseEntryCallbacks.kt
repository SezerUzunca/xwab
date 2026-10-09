package com.xwab.app.feature.browse.navigation

import com.xwab.app.core.sound.port.CategoryId
import org.jetbrains.compose.resources.StringResource

/** Application-owned labels and actions; this feature does not name destination features. */
class BrowseEntryCallbacks(
    val title: StringResource,
    val subtitle: StringResource,
    val onCategoryClick: (CategoryId) -> Unit,
)
