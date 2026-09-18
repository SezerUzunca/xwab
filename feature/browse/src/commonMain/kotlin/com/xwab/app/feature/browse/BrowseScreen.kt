package com.xwab.app.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xwab.feature.browse.generated.resources.Res
import xwab.feature.browse.generated.resources.app_subtitle
import xwab.feature.browse.generated.resources.app_title
import xwab.feature.browse.generated.resources.categories_title
import xwab.feature.browse.generated.resources.track_count

private val CATEGORY_CARD_MIN_WIDTH = 150.dp

@Composable
internal fun BrowseScreenRoute(
    onCategoryClick: (CategoryId) -> Unit,
    viewModel: BrowseViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val content = state) {
        BrowseUiState.Loading -> LoadingContent()
        is BrowseUiState.Ready -> BrowseScreen(content.value, onCategoryClick)
    }
}

@Composable
internal fun BrowseScreen(
    state: BrowseState,
    onCategoryClick: (CategoryId) -> Unit,
) {
    ScreenContainer {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(CATEGORY_CARD_MIN_WIDTH),
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = stringResource(Res.string.app_title),
                        style = SleepRelaxTheme.typography.headlineLarge,
                        color = SleepRelaxTheme.colors.textPrimary,
                    )
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                    Text(
                        text = stringResource(Res.string.app_subtitle),
                        style = SleepRelaxTheme.typography.bodyLarge,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingHuge))
                    Text(
                        text = stringResource(Res.string.categories_title),
                        style = SleepRelaxTheme.typography.bodySmall,
                        color = SleepRelaxTheme.colors.accent.copy(alpha = 0.75f),
                    )
                }
            }
            // `it.id.value`, not `it.id`: a lazy key is stored as `Any`, which boxes the value
            // class back into an object, and Android saves these keys into a `Bundle` that cannot
            // hold one. Every other list in the app already unwraps here; this one was missed since
            // the first commit, and it crashes the screen rather than degrading.
            items(state.categories, key = { it.id.value }) { category ->
                CategoryCard(category, onClick = { onCategoryClick(category.id) })
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CATEGORY_CARD_MIN_WIDTH)
            .clip(SleepRelaxTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(
                        SleepRelaxTheme.colors.glassWhite,
                        SleepRelaxTheme.colors.primary.copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable(onClick = onClick)
            .padding(SleepRelaxTheme.dimens.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingLarge),
    ) {
        Box(
            modifier = Modifier
                .size(SleepRelaxTheme.dimens.minimumTouchTarget)
                .clip(SleepRelaxTheme.shapes.small)
                .background(SleepRelaxTheme.colors.glassWhiteOverlay),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.symbol,
                color = SleepRelaxTheme.colors.accent.copy(alpha = 0.7f),
                style = SleepRelaxTheme.typography.titleLarge,
            )
        }
        Column {
            Text(category.name, style = SleepRelaxTheme.typography.titleSmall, color = SleepRelaxTheme.colors.textPrimary)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
            Text(
                category.description,
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
                maxLines = 1,
            )
            Text(
                pluralStringResource(Res.plurals.track_count, category.trackCount, category.trackCount),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.accent.copy(alpha = 0.55f),
            )
        }
    }
}

@Preview
@Composable
private fun BrowseScreenPreview() {
    SleepRelaxTheme {
        BrowseScreen(
            BrowseState(listOf(Category(CategoryId("rain"), "Rain", "Gentle raindrops", "☂", 1))),
            onCategoryClick = {},
        )
    }
}
