package com.xwab.app.di

import com.xwab.app.feature.browse.impl.di.BrowseDependencies
import com.xwab.app.feature.category.impl.di.CategoryDependencies
import com.xwab.app.feature.favorites.impl.di.FavoritesDependencies
import com.xwab.app.feature.sounds.impl.di.PlayerDependencies
import com.xwab.app.feature.story.impl.di.StoryDependencies

/**
 * What the application root can ask for: one bag of ports per screen, and nothing else.
 *
 * The graph itself is declared per platform — the Android one takes a `Context`, the iOS one takes
 * nothing — and both implement this. Metro merges every `@ContributesTo(AppScope::class)` in the
 * build into them, so no module list is maintained here the way `appModules()` used to be: a new
 * capability is a contributed interface in its own module and nothing else.
 *
 * Screens are absent on purpose. A ViewModel is internal to its feature, and a compile-time graph
 * can only expose what the module it is generated in can name.
 */
interface AppGraph {
    val browseDependencies: BrowseDependencies
    val favoritesDependencies: FavoritesDependencies
    val categoryDependencies: CategoryDependencies
    val playerDependencies: PlayerDependencies
    val storyDependencies: StoryDependencies
}
