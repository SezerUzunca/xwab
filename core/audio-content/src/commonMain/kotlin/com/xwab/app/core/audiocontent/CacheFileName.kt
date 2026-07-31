package com.xwab.app.core.audiocontent

/**
 * A cache file name is `<track id>-v<version>.mp3`, and it is the only string this module ever
 * hands to a file system. Both stores validate against this pattern before touching a path, so a
 * manifest typo cannot become a traversal.
 */
internal val CACHE_FILE_NAME = Regex("[a-z0-9-]+-v[0-9]+\\.mp3")

/**
 * The names in [existing] that a completed download of [current] has superseded: same track, older
 * version. Nothing else clears them, so without this a version bump would leave its predecessor on
 * disk for the life of the install.
 */
internal fun supersededCacheFileNames(existing: List<String>, current: String): List<String> {
    val trackPrefix = current.substringBeforeLast("-v", missingDelimiterValue = "")
    if (trackPrefix.isEmpty()) return emptyList()

    return existing.filter { name ->
        name != current && name.startsWith("$trackPrefix-v") && CACHE_FILE_NAME.matches(name)
    }
}
