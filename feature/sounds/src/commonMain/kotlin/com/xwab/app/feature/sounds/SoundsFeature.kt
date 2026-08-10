package com.xwab.app.feature.sounds

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.sounds.di.soundsModule
import com.xwab.app.feature.sounds.navigation.SoundsRoute
import com.xwab.app.feature.sounds.navigation.soundsNavigationSerializers
import org.jetbrains.compose.resources.stringResource
import xwab.feature.sounds.generated.resources.Res
import xwab.feature.sounds.generated.resources.tab_sounds

/** The whole of this feature, as the composition root sees it. */
val soundsFeature = FeatureEntry(
    koinModule = soundsModule,
    entries = { soundsEntry(it) },
    serializers = soundsNavigationSerializers,
    // Order 0, so this is the start destination: the tab back falls through to, and the one a back
    // press leaves the app from.
    topLevel = TopLevelDestination(
        route = SoundsRoute,
        order = 0,
        label = { stringResource(Res.string.tab_sounds) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
    ),
)
