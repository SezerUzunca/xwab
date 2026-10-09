package com.xwab.app.composition

import androidx.navigation3.runtime.EntryProviderScope
import com.xwab.app.feature.browse.navigation.BrowseEntryCallbacks
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryEntryCallbacks
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesEntryCallbacks
import com.xwab.app.feature.sound.navigation.SoundEntryCallbacks
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesEntryCallbacks
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.navigation.Navigator
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.app_subtitle
import xwab.shared.generated.resources.app_title

/**
 * Where each feature's intents lead: the one part of entry wiring the shell writes by hand.
 *
 * Contributed to the scope the composition root's entry graph aggregates, beside the features' own
 * installers, which receive these through their callback contracts. Destination mapping is app
 * policy, so it stays here rather than in any feature.
 */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object AppEntryCallbacks {
    @Provides
    fun provideBrowseCallbacks(navigator: Navigator): BrowseEntryCallbacks = BrowseEntryCallbacks(
        title = Res.string.app_title,
        subtitle = Res.string.app_subtitle,
        onCategoryClick = { navigator.navigate(CategoryRoute(it.value)) },
    )

    @Provides
    fun provideFavoritesCallbacks(navigator: Navigator): FavoritesEntryCallbacks = FavoritesEntryCallbacks(
        onTrackClick = { navigator.navigate(SoundRoute(it.value)) },
        onBrowse = { navigator.navigate(BrowseRoute) },
    )

    @Provides
    fun provideCategoryCallbacks(navigator: Navigator): CategoryEntryCallbacks = CategoryEntryCallbacks(
        onTrackClick = { navigator.navigate(SoundRoute(it.value)) },
        onBack = navigator::goBack,
    )

    @Provides
    fun provideSoundCallbacks(navigator: Navigator): SoundEntryCallbacks = SoundEntryCallbacks(
        onBack = navigator::goBack,
    )

    @Provides
    fun provideStoriesCallbacks(navigator: Navigator): StoriesEntryCallbacks = StoriesEntryCallbacks(
        onStoryClick = { navigator.navigate(StoryRoute(it.value)) },
        onBack = navigator::goBack,
    )
}
