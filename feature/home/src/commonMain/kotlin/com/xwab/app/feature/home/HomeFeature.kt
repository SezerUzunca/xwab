package com.xwab.app.feature.home

import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.feature.home.di.homeModule
import com.xwab.app.feature.home.navigation.homeNavigationSerializers

/** The whole of this feature, as the composition root sees it. */
val homeFeature = FeatureEntry(
    koinModule = homeModule,
    serializers = homeNavigationSerializers,
)
