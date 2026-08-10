package com.xwab.app.feature.category

import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.feature.category.di.categoryModule
import com.xwab.app.feature.category.navigation.categoryNavigationSerializers

/** The whole of this feature, as the composition root sees it. */
val categoryFeature = FeatureEntry(
    koinModule = categoryModule,
    entries = { categoryEntry(it) },
    serializers = categoryNavigationSerializers,
)
