package com.xwab.app.shell

import androidx.navigation3.runtime.EntryProviderScope
import com.xwab.app.feature.aftershift.navigation.AfterShiftEntryCallbacks
import com.xwab.app.feature.browse.navigation.BrowseEntryCallbacks
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.cantsleep.navigation.CantSleepEntryCallbacks
import com.xwab.app.feature.cantsleep.navigation.CantSleepRoute
import com.xwab.app.feature.category.navigation.CategoryEntryCallbacks
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesEntryCallbacks
import com.xwab.app.feature.nap.navigation.NapEntryCallbacks
import com.xwab.app.feature.nap.navigation.NapRoute
import com.xwab.app.feature.rehearsal.navigation.RehearsalEntryCallbacks
import com.xwab.app.feature.rehearsal.navigation.RehearsalRoute
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

    /** Lying awake, a nightmare and a nap open their own screens inside After Shift, so Back returns to the tab. */
    @Provides
    fun provideAfterShiftCallbacks(navigator: Navigator): AfterShiftEntryCallbacks = AfterShiftEntryCallbacks(
        onRewriteNightmare = { navigator.navigate(RehearsalRoute) },
        onCantSleep = { navigator.navigate(CantSleepRoute) },
        onNap = { navigator.navigate(NapRoute) },
    )

    @Provides
    fun provideNapCallbacks(navigator: Navigator): NapEntryCallbacks = NapEntryCallbacks(
        onBack = navigator::goBack,
    )

    @Provides
    fun provideCantSleepCallbacks(navigator: Navigator): CantSleepEntryCallbacks = CantSleepEntryCallbacks(
        onBack = navigator::goBack,
    )

    @Provides
    fun provideRehearsalCallbacks(navigator: Navigator): RehearsalEntryCallbacks = RehearsalEntryCallbacks(
        onBack = navigator::goBack,
    )

    @Provides
    fun provideStoriesCallbacks(navigator: Navigator): StoriesEntryCallbacks = StoriesEntryCallbacks(
        onStoryClick = { navigator.navigate(StoryRoute(it.value)) },
        onBack = navigator::goBack,
    )
}
