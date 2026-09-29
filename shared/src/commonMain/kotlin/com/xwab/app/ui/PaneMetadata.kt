package com.xwab.app.ui

import androidx.navigation3.runtime.NavMetadataKey
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole

/** Display policies travel with entries; unrelated lists and tabs must never form one scene. */
internal object TabKey : NavMetadataKey<String>
internal object ParentPaneKey : NavMetadataKey<ThreePaneScaffoldRole>
