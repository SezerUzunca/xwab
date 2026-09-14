package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/**
 * One row of something that can be played from the list it sits in, with whatever the session has
 * to say about it underneath.
 *
 * Every list in this app that plays inline draws the same three things: a [ContentCard], a transient
 * status line while a source is being resolved, and a failure message. They were written twice —
 * once for favourite sounds and once for stories — and had drifted: one indented its messages under
 * the card's text and the other did not. The indent is kept here because [ContentCard] pads its own
 * contents by the same token, so the message lines up with the title rather than the card edge.
 *
 * The category list was a third copy that never got that far: it drew its own card because its rows
 * carry a favorite control as well, and so it showed neither a status nor a failure — a tap on a
 * sound that could not be reached did nothing visible at all. [trailingContent] is what lets that
 * list draw its extra control without owning the other two lines again.
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
 * @param trailingContent drawn after the transport button, for a list whose rows carry one more
 *   control. Null where the transport button is the only one.
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
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ContentCard(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPauseClick)
                    trailingContent?.invoke()
                }
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
