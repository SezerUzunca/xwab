package com.xwab.app.feature.story

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.story.di.storyModule
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.storyNavigationSerializers
import org.jetbrains.compose.resources.stringResource
import xwab.feature.story.generated.resources.Res
import xwab.feature.story.generated.resources.tab_stories

/** The whole of this feature, as the composition root sees it. */
val storyFeature = FeatureEntry(
    koinModule = storyModule,
    serializers = storyNavigationSerializers,
    topLevel = TopLevelDestination(
        route = StoriesRoute,
        order = 1,
        label = { stringResource(Res.string.tab_stories) },
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
    ),
)
