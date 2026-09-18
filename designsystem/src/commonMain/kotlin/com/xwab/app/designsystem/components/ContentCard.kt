package com.xwab.app.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/**
 * A titled, subtitled glass card that does something when it is tapped.
 *
 * Internal because [PlayableRow] is the only thing that draws one, and the only shape this app
 * wants a card in: a card carrying a transport control and whatever the session has to say about
 * it. A screen reaching for the bare card again is how the status and failure lines went missing
 * from the category list the first time.
 */
@Composable
internal fun ContentCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .clickable(onClick = onClick)
            .padding(SleepRelaxTheme.dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SleepRelaxTheme.typography.titleMedium,
                color = SleepRelaxTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent?.let {
            Spacer(Modifier.width(SleepRelaxTheme.dimens.spacingSmall))
            it()
        }
    }
}
