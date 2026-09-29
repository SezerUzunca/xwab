package com.xwab.app.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.get
import com.xwab.app.designsystem.components.LocalBackButtonAction
import com.xwab.app.designsystem.components.LocalBackButtonVisibility

/**
 * Hide Up only while the actual parent pane is visible; compact destinations retain their control.
 *
 * While more than one pane is on screen, the arrow closes the pane it is drawn on ([onUp]). Back
 * would close the latest pane instead, which is a different one whenever this is not the rightmost.
 * A single visible pane keeps the destination's own action.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AdaptiveBackControl(metadata: Map<String, Any>, onUp: () -> Unit, content: @Composable () -> Unit) {
    val parent = metadata[ParentPaneKey]
    val scaffold = LocalListDetailSceneScope.current?.scaffoldTransitionScope?.scaffoldStateTransition?.targetState
    val parentVisible = parent != null && scaffold?.get(parent) == PaneAdaptedValue.Expanded
    val multiplePanes = scaffold != null && PANE_ROLES.count { scaffold[it] == PaneAdaptedValue.Expanded } > 1
    val up = dropUnlessResumed(block = onUp)
    CompositionLocalProvider(
        LocalBackButtonVisibility provides !parentVisible,
        LocalBackButtonAction provides up.takeIf { multiplePanes },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val PANE_ROLES = listOf(
    ListDetailPaneScaffoldRole.List,
    ListDetailPaneScaffoldRole.Detail,
    ListDetailPaneScaffoldRole.Extra,
)
