package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.add_favorite
import xwab.designsystem.generated.resources.remove_favorite
import xwab.designsystem.generated.resources.favorites_loading

/**
 * @param enabled whether the caller can change this favorite.
 * @param isLoading shows progress in place of an unknown favorite value and disables the action.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val loadingDescription = stringResource(Res.string.favorites_loading)
    IconButton(
        onClick = onClick,
        // A waiting favorite is described on the button, not on the spinner inside it. Material's
        // indeterminate indicator carries `progressSemantics`, which merges its own descendants and
        // therefore stands as a node of its own instead of folding into the button. Described
        // there, the label named a node with no enabled state to announce: a reader found the
        // spinner and never the control it was disabling, and an assertion about the control being
        // refused had nothing to assert on.
        modifier = if (isLoading) {
            modifier.semantics { contentDescription = loadingDescription }
        } else {
            modifier
        },
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SleepRelaxTheme.dimens.iconSmall),
                color = SleepRelaxTheme.colors.textSecondary,
            )
        } else Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) Res.string.remove_favorite else Res.string.add_favorite,
            ),
            tint = when {
                !enabled -> SleepRelaxTheme.colors.textSecondary.copy(alpha = DISABLED_ALPHA)
                isFavorite -> SleepRelaxTheme.colors.accent
                else -> SleepRelaxTheme.colors.textSecondary
            },
        )
    }
}
