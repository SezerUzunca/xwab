package com.xwab.app.core.audiodelivery.cache

import com.xwab.app.core.catalogmanifest.CACHE_FILE_NAME

/**
 * The name a transfer is staged under until it is complete.
 *
 * Deliberately fails [CACHE_FILE_NAME]: [find][AudioFileStore.find] must not report it as a cache
 * hit, and [unreferencedCacheFileNames] must not sweep it out from under a running transfer.
 *
 * Staging is delivery's business rather than the catalog's — the catalog knows the name a finished
 * track lives under, and nothing about how the bytes get there.
 */
internal fun partialCacheFileName(cacheFileName: String): String = ".$cacheFileName.part"

/**
 * The names in [existing] that [keep] no longer refers to. One rule covers both ways a cached file
 * goes stale — a raised version and a track dropped from the catalog — because in either case the
 * name simply stops being asked for. Nothing else clears them, so without this a build that changes
 * the manifest would leave its predecessors on disk for the life of the install.
 *
 * [keep] comes from `AudioSourceCatalog.cacheFileNames`, so the catalog stays the one place that
 * decides which tracks exist.
 *
 * Only well-formed names are returned. A download in progress is staged under a name that fails
 * [CACHE_FILE_NAME], so a sweep can never pull a file out from under a running transfer.
 */
internal fun unreferencedCacheFileNames(existing: List<String>, keep: Set<String>): List<String> =
    existing.filter { name -> name !in keep && CACHE_FILE_NAME.matches(name) }
