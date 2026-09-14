package com.xwab.app.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.add_favorite
import xwab.designsystem.generated.resources.remove_favorite

/**
 * @param enabled false where there is nothing to favorite. A row in a list always has something,
 *   so it is the screen for one sound that passes this: a sound the catalog no longer holds used
 *   to draw a heart that looked live and did nothing, because the refusal lived in the ViewModel
 *   and the drawing did not know about it.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(
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
