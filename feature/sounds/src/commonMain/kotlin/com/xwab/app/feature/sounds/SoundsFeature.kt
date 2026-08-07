package com.xwab.app.feature.sounds

import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.feature.sounds.di.soundsModule
import com.xwab.app.feature.sounds.navigation.soundsNavigationSerializers

/** The whole of this feature, as the composition root sees it. */
val soundsFeature = FeatureEntry(
    koinModule = soundsModule,
    serializers = soundsNavigationSerializers,
)
