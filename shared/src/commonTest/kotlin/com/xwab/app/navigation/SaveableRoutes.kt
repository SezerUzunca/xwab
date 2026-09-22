package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute

/**
 * Every route this app can put on a saved back stack, written out once.
 *
 * Two tests need it and they check opposite halves of the same contract: FeatureSerializersTest
 * that each route survives being saved and read back, AppEntryProviderTest that each one has a
 * screen to draw when it comes back. Stated once so a new route is added in one place rather than
 * two that can quietly disagree.
 *
 * This list is still hand-written — it has to be, since neither `entryProvider` nor a
 * `SerializersModule` can be asked to enumerate itself in a way that would catch its own omission.
 * What keeps it honest is `theRouteListIsEveryRouteTheModuleActuallyRegisters`, which reads the
 * registrations back out of `FEATURE_SERIALIZERS` and fails when the two sets differ.
 *
 * Argument values are irrelevant: both lookups are by type, not by content.
 */
internal val SAVEABLE_ROUTES: List<NavKey> = listOf(
    BrowseRoute,
    FavoritesRoute,
    StoriesRoute,
    CategoryRoute("any-category"),
    SoundRoute("any-track"),
)
