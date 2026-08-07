package com.xwab.app.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.home.di.homeModule
import com.xwab.app.feature.home.navigation.HomeRoute
import com.xwab.app.feature.home.navigation.homeNavigationSerializers
import org.jetbrains.compose.resources.stringResource
import xwab.feature.home.generated.resources.Res
import xwab.feature.home.generated.resources.tab_sounds

/** The whole of this feature, as the composition root sees it. */
val homeFeature = FeatureEntry(
    koinModule = homeModule,
    serializers = homeNavigationSerializers,
    // Order 0, so this is the start destination: the tab back falls through to, and the one a back
    // press leaves the app from.
    topLevel = TopLevelDestination(
        route = HomeRoute,
        order = 0,
        label = { stringResource(Res.string.tab_sounds) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
    ),
)
