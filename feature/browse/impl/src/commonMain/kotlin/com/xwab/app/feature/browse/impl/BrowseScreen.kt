package com.xwab.app.feature.browse.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.ui.components.LoadingContent
import com.xwab.app.core.ui.components.SleepRelaxBackground
import com.xwab.app.core.ui.state.Loadable
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xwab.feature.browse.`impl`.generated.resources.Res
import xwab.feature.browse.`impl`.generated.resources.app_subtitle
import xwab.feature.browse.`impl`.generated.resources.app_title
import xwab.feature.browse.`impl`.generated.resources.categories_title
import xwab.feature.browse.`impl`.generated.resources.track_count

@Composable
internal fun BrowseScreenRoute(component: BrowseComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> BrowseScreen(content.value, component.onCategoryClick)
    }
}

@Composable
internal fun BrowseScreen(
    state: BrowseState,
    onCategoryClick: (CategoryId) -> Unit,
) {
    SleepRelaxBackground {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(SleepRelaxTheme.dimens.categoryCardMinWidth),
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                end = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                top = SleepRelaxTheme.dimens.paddingScreenVertical,
                bottom = SleepRelaxTheme.dimens.spacingHuge,
            ),
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
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.playIconCircleSize))
                    Text(
                        text = stringResource(Res.string.categories_title),
                        style = SleepRelaxTheme.typography.bodySmall,
                        color = SleepRelaxTheme.colors.accent.copy(alpha = 0.75f),
                    )
                }
            }
            items(state.categories, key = { it.id }) { category ->
                CategoryCard(category, onClick = { onCategoryClick(category.id) })
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
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
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
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
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(category.name, style = SleepRelaxTheme.typography.titleSmall, color = SleepRelaxTheme.colors.textPrimary)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
            Text(
                category.description,
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
                maxLines = 1,
            )
            Text(
                pluralStringResource(Res.plurals.track_count, category.musicCount, category.musicCount),
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
