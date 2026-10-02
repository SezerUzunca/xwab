package com.xwab.app.core.delivery

import okio.Path

/** Where this platform keeps the content cache. Each platform contributes its own. */
internal interface ContentCacheLocation {
    /** The directory the content cache lives in. */
    val root: Path

    /**
     * Cache directories this module used to write to and no longer reads. The store drops them
     * once, on its first download.
     */
    val legacyRoots: List<Path>
}
