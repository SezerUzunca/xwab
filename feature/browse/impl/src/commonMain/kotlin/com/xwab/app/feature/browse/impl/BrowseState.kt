package com.xwab.app.feature.browse.impl

import com.xwab.app.core.catalog.Category

internal data class BrowseState(
    val categories: List<Category> = emptyList(),
)
