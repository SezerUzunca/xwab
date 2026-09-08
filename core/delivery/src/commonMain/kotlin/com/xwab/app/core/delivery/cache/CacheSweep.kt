package com.xwab.app.core.delivery.cache

/** Staged names cannot be cache keys and are excluded from inventory cleanup. */
internal fun partialCacheFileName(cacheFileName: String): String = ".$cacheFileName.part"

/** The caller supplies only files in one namespace and its complete current inventory. */
internal fun unreferencedCacheFileNames(existing: List<String>, keep: Set<String>): List<String> =
    existing.filter { name -> name !in keep && CACHE_FILE_NAME.matches(name) }
