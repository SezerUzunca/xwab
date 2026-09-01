package com.xwab.app.feature.category.api.navigation

import kotlinx.serialization.Serializable

/** Which category screen to show. */
@Serializable
data class CategoryConfig(val categoryId: String)
