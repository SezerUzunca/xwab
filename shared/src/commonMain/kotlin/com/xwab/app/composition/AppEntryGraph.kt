package com.xwab.app.composition

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEventBus
import com.xwab.app.feature.browse.navigation.BrowseEntryCallbacks
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryEntryCallbacks
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesEntryCallbacks
import com.xwab.app.feature.sound.navigation.SoundEntryCallbacks
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesEntryCallbacks
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.navigation.NavigationState
import com.xwab.app.navigation.Navigator
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.app_subtitle
import xwab.shared.generated.resources.app_title

internal typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit

/**
 * Collects the entry installers features contribute, for one host, independently of the ViewModel
 * graph.
 *
 * Features contribute with `@ContributesTo(EntryProviderScope::class)`, so a feature on shared's
 * classpath is installed without being listed here. The scope is Navigation 3's own type because
 * it is the one class both sides already see; a marker of the app's own would need a module of its
 * own. The callbacks below are what remains here: where each feature's intents lead is app policy.
 *
 * The input is the [NavigationState] Compose restored; the graph builds the one [Navigator] over it
 * that the host and every feature callback share. The host remembers one graph per state. Kept
 * here, outside any feature's reach, the navigator stays the only place destinations are chosen.
 */
@DependencyGraph(EntryProviderScope::class)
internal interface AppEntryGraph {
    val navigator: Navigator
    val resultEventBus: ResultEventBus
    val entryProviderInstallers: Set<EntryProviderInstaller>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides state: NavigationState): AppEntryGraph
    }

    @Provides
    @SingleIn(EntryProviderScope::class)
    fun provideResultEventBus(): ResultEventBus = ResultEventBus()

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
