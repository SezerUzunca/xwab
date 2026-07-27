package com.xwab.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.core.ui.generated.resources.Res
import xwab.core.ui.generated.resources.add_favorite
import xwab.core.ui.generated.resources.remove_favorite

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) Res.string.remove_favorite else Res.string.add_favorite,
            ),
            tint = if (isFavorite) SleepRelaxTheme.colors.accent else SleepRelaxTheme.colors.textSecondary,
        )
    }
}
