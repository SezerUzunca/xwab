package com.xwab.app.core.catalogmanifest

/**
 * A cache file name is `<track id>-v<version>.mp3`, and it is the only string the catalog ever
 * hands towards a file system. [CatalogEntry] refuses a track id that cannot form one, and
 * `core:sound:delivery` checks again before a name reaches a path, so a manifest typo cannot
 * become a traversal.
 *
 * Public rather than internal because the name is the contract between this module and delivery:
 * the catalog produces it, the cache consumes it, and both have to agree on what a well-formed one
 * looks like. A second copy of the pattern on the delivery side is precisely how the two would
 * drift apart.
 */
val CACHE_FILE_NAME = Regex("[a-z0-9-]+-v[0-9]+\\.mp3")
