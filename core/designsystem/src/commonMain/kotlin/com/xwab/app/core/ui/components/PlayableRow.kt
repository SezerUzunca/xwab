package com.xwab.app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.core.ui.theme.SleepRelaxTheme

/**
 * One row of something that can be played from the list it sits in, with whatever the session has
 * to say about it underneath.
 *
 * Every list in this app that plays inline draws the same three things: a [MusicCard], a transient
 * status line while a source is being resolved, and a failure message. They were written twice —
 * once for favourite sounds and once for stories — and had drifted: one indented its messages under
 * the card's text and the other did not. The indent is kept here because [MusicCard] pads its own
 * contents by the same token, so the message lines up with the title rather than the card edge.
 *
 * Content-neutral on purpose. It takes strings, not a catalog model and not a `PlaybackFailure`:
 * the wording for "this could not be reached" belongs to whichever feature knows whether the thing
 * is a sound or a story, and the design system has no business deciding that.
 *
 * @param onClick what tapping the row itself does — usually opening the thing.
 * @param onPlayPauseClick what the transport button does. Pass the same lambda as [onClick] for a
 *   list whose rows only play, like the story list, where there is nothing else to open.
 * @param statusMessage shown while the row is wanted but not yet audible. Null when it is not.
 * @param errorMessage shown when the session failed on this row. Null when it did not.
 */
@Composable
fun PlayableRow(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    errorMessage: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MusicCard(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailingContent = {
                PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPauseClick)
            },
        )
        statusMessage?.let {
            Text(
                text = it,
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
                modifier = Modifier.padding(
                    start = SleepRelaxTheme.dimens.spacingMedium,
                    top = SleepRelaxTheme.dimens.spacingExtraSmall,
                ),
            )
        }
        errorMessage?.let {
            Text(
                text = it,
                style = SleepRelaxTheme.typography.bodyMedium,
                color = SleepRelaxTheme.colors.error,
                modifier = Modifier.padding(
                    start = SleepRelaxTheme.dimens.spacingMedium,
                    top = SleepRelaxTheme.dimens.spacingExtraSmall,
                ),
            )
        }
    }
}
