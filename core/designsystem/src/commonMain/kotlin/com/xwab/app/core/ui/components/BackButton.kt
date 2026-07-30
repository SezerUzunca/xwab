package com.xwab.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.core.designsystem.generated.resources.Res
import xwab.core.designsystem.generated.resources.back

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.back),
            tint = SleepRelaxTheme.colors.textSecondary,
        )
    }
}
