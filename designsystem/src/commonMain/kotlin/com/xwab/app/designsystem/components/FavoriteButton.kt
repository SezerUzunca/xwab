package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.favorite
import xwab.designsystem.generated.resources.favorite_content
import xwab.designsystem.generated.resources.favorites_loading

/**
 * @param enabled whether the caller can change this favorite.
 * @param isLoading shows progress in place of an unknown favorite value and disables the action.
 */
@Composable
// Loading, enabled state and the accessible title are independent options for this reusable control.
@Suppress("LongParameterList")
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentTitle: String? = null,
) {
    val loadingDescription = stringResource(Res.string.favorites_loading)
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = { onClick() },
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
            // A toggle's name stays put; its checked state is what a screen reader announces as on or
            // off. An action phrase here would read "Remove … from favorites, checked".
            contentDescription = if (contentTitle == null) {
                stringResource(Res.string.favorite)
            } else {
                stringResource(Res.string.favorite_content, contentTitle)
            },
            tint = when {
                !enabled -> SleepRelaxTheme.colors.textSecondary.copy(alpha = DISABLED_ALPHA)
                isFavorite -> SleepRelaxTheme.colors.accent
                else -> SleepRelaxTheme.colors.textSecondary
            },
        )
    }
}

@Preview
@Composable
private fun FavoriteButtonPreview() {
    SleepRelaxTheme {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FavoriteButton(isFavorite = false, onClick = {}, contentTitle = "Rain")
            FavoriteButton(isFavorite = true, onClick = {}, contentTitle = "Rain")
            FavoriteButton(isFavorite = false, onClick = {}, isLoading = true)
            FavoriteButton(isFavorite = true, onClick = {}, enabled = false)
        }
    }
}
