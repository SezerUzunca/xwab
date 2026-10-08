package com.xwab.app.composition

import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.category
import com.xwab.app.testing.track
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

/**
 * The sound catalog the app would download, replaced so a scenario runs offline.
 *
 * Only the catalog: it is the one port that reads the network before anything plays. Favorites,
 * playback and everything between the graph and the screen stay the platform's production code.
 */
@BindingContainer
internal class OfflineCatalog {
    @Provides
    fun sound(): SoundPort = FakeSoundCatalog(
        categories = listOf(category(CATEGORY, trackCount = 1)),
        tracks = listOf(track(TRACK, categoryId = CATEGORY)),
    )

    companion object {
        const val CATEGORY = "night-rain"
        const val TRACK = "ocean-waves"
    }
}
