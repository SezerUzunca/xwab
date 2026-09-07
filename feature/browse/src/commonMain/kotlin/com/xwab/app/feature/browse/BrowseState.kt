package com.xwab.app.feature.browse

import com.xwab.app.core.sound.port.Category

internal data class BrowseState(
    val categories: List<Category> = emptyList(),
)
