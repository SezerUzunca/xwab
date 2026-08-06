package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.Music

/**
 * One catalog track and the permanent HTTPS source its audio comes from.
 *
 * Every track is remote — nothing ships inside the app — so a first play streams while the copy is
 * cached for later. [version] is part of the cache file name, so raising it retires the previously
 * cached file instead of serving stale audio.
 */
internal class CatalogEntry(
    val music: Music,
    val httpsUrl: String,
    val version: Int = 1,
) {
    /**
     * Held rather than derived on each read, so that the check below covers every entry that
     * exists: an id the file stores would refuse is caught where the entry is written, not on the
     * device where the refusal would surface as a failed tap.
     */
    val cacheFileName: String = "${music.id.value}-v$version.mp3"

    init {
        require(httpsUrl.startsWith("https://")) { "Catalog audio must use HTTPS." }
        // The pattern rejects a malformed or empty id and a negative version, but accepts v0.
        require(version > 0) { "Catalog audio versions must be positive." }
        require(CACHE_FILE_NAME.matches(cacheFileName)) {
            "Catalog track ids must cache under a safe file name: $cacheFileName"
        }
    }
}
