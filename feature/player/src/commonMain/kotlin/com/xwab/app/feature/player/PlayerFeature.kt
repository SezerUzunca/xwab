package com.xwab.app.feature.player

import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.feature.player.di.playerModule
import com.xwab.app.feature.player.navigation.playerNavigationSerializers

/** The whole of this feature, as the composition root sees it. */
val playerFeature = FeatureEntry(
    koinModule = playerModule,
    serializers = playerNavigationSerializers,
)
