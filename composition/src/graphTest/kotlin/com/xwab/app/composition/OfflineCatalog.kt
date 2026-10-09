package com.xwab.app.composition

import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.story.port.StoryPort
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.category
import com.xwab.app.testing.track
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The data a scenario reads, replaced so it runs offline, writes nothing to the device and knows
 * what it will find.
 *
 * The sound catalog is the one port that reads the network before anything plays. Favorites start
 * with [TRACK] and live in memory, so the Favorites tab has a row and no run leaves one behind. The
 * story catalog holds one known story. Playback, delivery and everything between the graph and the
 * screen stay the platform's production code.
 */
@BindingContainer
internal class OfflineCatalog {
    @Provides
    fun sound(): SoundPort = FakeSoundCatalog(
        categories = listOf(category(CATEGORY, trackCount = 1)) +
            List(FILLER_CATEGORIES) { category("category-$it", trackCount = 0) },
        tracks = listOf(track(TRACK, categoryId = CATEGORY)),
    )

    @Provides
    fun favorites(): FavoritesPort = FakeFavorites(setOf(TrackId(TRACK)))

    @Provides
    fun stories(): StoryPort = object : StoryPort {
        private val story = Story(
            id = StoryId(STORY_ID),
            title = STORY,
            author = "Test Author",
            description = "A story the scenario opens.",
            narrator = NARRATOR,
            durationSeconds = 600,
        )

        override fun observeStories(): Flow<List<Story>> = flowOf(listOf(story))

        override fun observeStory(storyId: StoryId): Flow<Story?> =
            flowOf(story.takeIf { it.id == storyId })
    }

    companion object {
        const val CATEGORY = "night-rain"
        const val TRACK = "ocean-waves"
        const val STORY_ID = "night-train"
        const val STORY = "The Night Train"
        const val NARRATOR = "Test Narrator"

        /** Empty categories after [CATEGORY]: enough that scrolling to the last pushes it off screen. */
        const val FILLER_CATEGORIES = 60
    }
}
