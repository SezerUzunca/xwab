package com.xwab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.preparing
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.now_playing

/**
 * What the session is on, on every screen, with the one control that is true of all of them.
 *
 * Application chrome, beside [AppNavigationBar] and for the same reason: a listener who starts a
 * sound from a category list and then walks over to Browse had nothing to stop it with, because
 * the only transport controls in the app belonged to the row and the screen that started it.
 *
 * It cannot be a feature. A feature publishes a Navigation 3 route and nothing else, and this is
 * not a destination — it is drawn outside `NavDisplay`, above the navigation bar, whatever is on
 * screen. So it lives where the rest of the shell's chrome does.
 *
 * Content-neutral, like everything the session publishes: it is handed a title and an intent, and
 * never asks whether the thing playing is a sound or a story. That is also why tapping it does not
 * open anything — "open this item" has a different answer per kind, and one of those kinds has no
 * screen to open at all.
 *
 * @param title what is playing, or null while the session has not got a name for it yet.
 * @param isPlaying the session's *intent*, not audible sound — the same value the button acts on,
 *   so the icon and the tap cannot disagree.
 * @param isPreparing the item is wanted but not audible yet; the line that says so replaces the
 *   title, which by contract is absent for exactly as long.
 */
@Composable
internal fun NowPlayingBar(
    title: String?,
    isPlaying: Boolean,
    isPreparing: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = SleepRelaxTheme.colors.glassWhiteOverlay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleepRelaxTheme.colors.backgroundBottom)
                .padding(
                    start = SleepRelaxTheme.dimens.spacingLarge,
                    end = SleepRelaxTheme.dimens.spacingSmall,
                    top = SleepRelaxTheme.dimens.spacingSmall,
                    bottom = SleepRelaxTheme.dimens.spacingSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title ?: stringResource(Res.string.now_playing),
                    style = SleepRelaxTheme.typography.titleSmall,
                    color = SleepRelaxTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // The same word every list uses for the same state, taken from the design system
                // rather than spelled again here.
                if (isPreparing) {
                    Text(
                        text = stringResource(UiRes.string.preparing),
                        style = SleepRelaxTheme.typography.labelMedium,
                        color = SleepRelaxTheme.colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPauseClick)
        }
    }
}
