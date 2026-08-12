package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The entry is where a hand-written manifest row becomes a file name, so what it refuses here is
 * what the file stores would otherwise refuse on a device, in the middle of a tap.
 */
class CatalogEntryTest {
    @Test
    fun theCacheFileNameCarriesTheTrackIdAndItsVersion() {
        assertEquals("heavy-rain-v2.mp3", entry(id = "heavy-rain", version = 2).cacheFileName)
    }

    @Test
    fun anIdThatCannotBecomeASafeFileNameIsRejected() {
        assertFailsWith<IllegalArgumentException> { entry(id = "White_Noise") }
        assertFailsWith<IllegalArgumentException> { entry(id = "../etc/passwd") }
        assertFailsWith<IllegalArgumentException> { entry(id = "") }
    }

    /** The name pattern accepts `v0`, so this bound is the entry's to hold. */
    @Test
    fun aVersionBelowOneIsRejected() {
        assertFailsWith<IllegalArgumentException> { entry(version = 0) }
        assertFailsWith<IllegalArgumentException> { entry(version = -1) }
    }

    @Test
    fun aSourceThatIsNotHttpsIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            entry(httpsUrl = "http://example.test/heavy-rain.mp3")
        }
    }

    private fun entry(
        id: String = "heavy-rain",
        version: Int = 1,
        httpsUrl: String = "https://example.test/heavy-rain.mp3",
    ) = CatalogEntry(Music(TrackId(id), "Heavy Rain", CategoryId("rain"), 30), httpsUrl, version)
}
