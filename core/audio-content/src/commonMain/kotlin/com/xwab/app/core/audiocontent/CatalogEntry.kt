package com.xwab.app.core.audiocontent

import com.xwab.app.core.model.Music

/**
 * One catalog track and the permanent HTTPS source its audio comes from.
 *
 * Every track is remote — nothing ships inside the app — so a first play streams while the copy is
 * cached for later. [version] is part of the cache file name, so raising it retires the previously
 * cached file instead of serving stale audio.
 */
internal data class CatalogEntry(
    val music: Music,
    val httpsUrl: String,
    val version: Int = 1,
) {
    init {
        require(httpsUrl.startsWith("https://")) { "Catalog audio must use HTTPS." }
        require(version > 0) { "Catalog audio versions must be positive." }
    }

    val cacheFileName: String get() = "${music.id}-v$version.mp3"
}
