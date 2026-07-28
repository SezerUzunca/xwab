package com.xwab.app.core.audiocontent

import com.xwab.app.core.model.Music

internal data class CatalogEntry(
    val music: Music,
    val asset: AudioAssetSpec,
)

internal sealed interface AudioAssetSpec {
    data class Bundled(val resourcePath: String) : AudioAssetSpec {
        init {
            require(resourcePath.startsWith("files/audio/")) {
                "Bundled audio must live below files/audio/."
            }
        }
    }

    data class Remote(
        val httpsUrl: String,
        val version: Int = 1,
    ) : AudioAssetSpec {
        init {
            require(httpsUrl.startsWith("https://")) { "Remote audio must use HTTPS." }
            require(version > 0) { "Remote audio versions must be positive." }
        }

        fun cacheFileName(musicId: String): String = "$musicId-v$version.mp3"
    }
}
