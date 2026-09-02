package com.xwab.app.feature.browse.impl

import com.xwab.app.core.catalog.Category

data class BrowseState(
    val categories: List<Category> = emptyList(),
)
