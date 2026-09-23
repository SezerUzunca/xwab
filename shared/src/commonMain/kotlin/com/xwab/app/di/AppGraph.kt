package com.xwab.app.di

import com.xwab.app.content.ContentCacheMaintenance
import com.xwab.app.feature.browse.di.BrowseDependencies
import com.xwab.app.feature.category.di.CategoryDependencies
import com.xwab.app.feature.favorites.di.FavoritesDependencies
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies
import com.xwab.app.feature.sound.di.SoundDependencies
import com.xwab.app.feature.story.di.StoriesDependencies

/**
 * Deferred access to one bag of ports per screen. Metro creates the bag on invocation, so
 * registering navigation entries does not initialize unopened features' dependencies.
 *
 * The graph itself is declared per platform — the Android one takes a `Context`, the iOS one takes
 * nothing — and both implement this. Metro merges each adapter's generated contribution provider
 * into them, so no *binding* is named here: a new capability contributes its own port binding, and
 * what this module adds is the dependency on the module that holds it. Metro aggregates a scope's
 * contributions from the compile classpath, so a capability whose module is missing from
 * `shared/build.gradle.kts` never reaches the graph.
 *
 * Screens are absent on purpose. A ViewModel is internal to its feature, and a compile-time graph
 * can only expose what the module it is generated in can name.
 */
interface AppGraph {
    val browseDependencies: () -> BrowseDependencies
    val favoritesDependencies: () -> FavoritesDependencies
    val categoryDependencies: () -> CategoryDependencies
    val soundDependencies: () -> SoundDependencies
    val storiesDependencies: () -> StoriesDependencies

    /**
     * The now-playing bar, which is chrome rather than a destination — so unlike the five above,
     * this one is invoked on the first frame rather than when a route is opened.
     *
     * Worth knowing that this moves the session's creation to app start: on Android that is when
     * the MediaController binds to the playback service, where it used to wait for the first screen
     * that reads playback.
     */
    val nowPlayingDependencies: () -> NowPlayingDependencies

    /**
     * Not a screen: the one piece of housekeeping that has to happen whether anything is opened
     * or not. Removing a content module leaves its downloads behind, and only this module knows
     * which namespaces are still installed.
     */
    val contentCacheMaintenance: () -> ContentCacheMaintenance
}
