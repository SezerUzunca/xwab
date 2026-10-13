package com.xwab.app.feature.shuffle

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.SleepTimerCard
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.shuffle.navigation.ShuffleRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.feature.shuffle.generated.resources.Res
import xwab.feature.shuffle.generated.resources.shuffle_could_not_play
import xwab.feature.shuffle.generated.resources.shuffle_empty
import xwab.feature.shuffle.generated.resources.shuffle_how_title
import xwab.feature.shuffle.generated.resources.shuffle_not_found
import xwab.feature.shuffle.generated.resources.shuffle_pace
import xwab.feature.shuffle.generated.resources.shuffle_preparing
import xwab.feature.shuffle.generated.resources.shuffle_research_note
import xwab.feature.shuffle.generated.resources.shuffle_step_drift
import xwab.feature.shuffle.generated.resources.shuffle_step_let_go
import xwab.feature.shuffle.generated.resources.shuffle_step_listen
import xwab.feature.shuffle.generated.resources.shuffle_step_picture
import xwab.feature.shuffle.generated.resources.shuffle_subtitle
import xwab.feature.shuffle.generated.resources.shuffle_title
import xwab.feature.shuffle.generated.resources.shuffle_unavailable

@Composable
internal fun ShuffleScreenRoute(viewModel: ShuffleViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Another tap on the tab at its root asks for the start, as every tab's root does.
    ResultEffect<ShuffleRoute> { scope.launch { scrollState.animateScrollTo(0) } }

    when (val content = state) {
        ShuffleUiState.Loading -> LoadingContent()
        is ShuffleUiState.Ready -> ShuffleScreen(
            state = content.value,
            onPlaybackClick = viewModel::togglePlayback,
            onTimerStart = viewModel::startSleepTimer,
            onTimerCancel = viewModel::cancelSleepTimer,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            scrollState = scrollState,
        )
    }
}

/**
 * A tab's root, so there is no back button. Each deck gets the same treatment as a sound or a story
 * on its own screen — its name, its one action, what the session says about it — then the session's
 * sleep timer, and below them how to listen, since the technique asks something of the listener.
 */
@Composable
@Suppress("LongParameterList") // Screen events plus the timer the card reports.
internal fun ShuffleScreen(
    state: ShuffleState,
    onPlaybackClick: (ShuffleDeckId) -> Unit,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    sleepTimerRemainingMs: Long? = null,
    scrollState: ScrollState = rememberScrollState(),
) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(screenContentPadding()),
        ) {
            Text(
                text = stringResource(Res.string.shuffle_title),
                modifier = Modifier.semantics { heading() },
                // The same size as the other tab titles; a tab root is not a sub-page.
                style = SleepRelaxTheme.typography.headlineLarge,
                color = SleepRelaxTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
            Text(
                text = stringResource(Res.string.shuffle_subtitle),
                style = SleepRelaxTheme.typography.bodyLarge,
                color = SleepRelaxTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))

            if (state.decks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.shuffle_empty),
                    style = SleepRelaxTheme.typography.bodyMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
            }
            state.decks.forEach { deck ->
                DeckCard(deck = deck, state = state, onPlaybackClick = { onPlaybackClick(deck.id) })
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            }

            SleepTimerCard(
                remainingMs = sleepTimerRemainingMs,
                onTimerStart = onTimerStart,
                onTimerCancel = onTimerCancel,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            HowToListen()
        }
    }
}

@Composable
private fun DeckCard(deck: ShuffleDeck, state: ShuffleState, onPlaybackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            deck.title,
            modifier = Modifier.semantics { heading() },
            style = SleepRelaxTheme.typography.headlineSmall,
            color = SleepRelaxTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            deck.description,
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(Res.string.shuffle_pace, deck.wordIntervalSeconds),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
        PlayPauseButton(
            playRequested = state.isDeckPlaying(deck.id),
            onClick = onPlaybackClick,
            large = true,
            contentTitle = deck.title,
        )
        DeckStatus(deck.id, state)
    }
}

@Composable
private fun DeckStatus(deckId: ShuffleDeckId, state: ShuffleState) {
    if (state.isDeckPreparing(deckId)) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
        Text(
            stringResource(Res.string.shuffle_preparing),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    state.deckFailure(deckId)?.let { failure ->
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(failure.messageResource()),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HowToListen() {
    Text(
        stringResource(Res.string.shuffle_how_title),
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.titleMedium,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    listOf(
        Res.string.shuffle_step_listen,
        Res.string.shuffle_step_picture,
        Res.string.shuffle_step_let_go,
        Res.string.shuffle_step_drift,
    ).forEachIndexed { index, step ->
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            "${index + 1}. ${stringResource(step)}",
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textPrimary,
        )
    }
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
    Text(
        stringResource(Res.string.shuffle_research_note),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}

/**
 * A deck the catalog dropped, one whose words could not be spoken and one the player could not open
 * call for different advice: only the second is worth checking the device's voice for.
 */
internal fun PlaybackFailure.messageResource(): StringResource = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.shuffle_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.shuffle_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.shuffle_could_not_play
}

@Preview
@Composable
private fun ShuffleScreenPreview() {
    SleepRelaxTheme {
        ShuffleScreen(
            state = ShuffleState(
                decks = listOf(
                    ShuffleDeck(
                        id = ShuffleDeckId("everyday-things"),
                        title = "Everyday Things",
                        description = "Ordinary objects, animals and places, one at a time and in no particular order.",
                        languageTag = "en-US",
                        wordIntervalSeconds = 8,
                    ),
                ),
            ),
            onPlaybackClick = {},
            onTimerStart = {},
            onTimerCancel = {},
        )
    }
}
