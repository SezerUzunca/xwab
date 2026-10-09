package com.xwab.app.core.delivery

import okio.Path

/** Where this platform keeps the content cache. Each platform's graph binds its own. */
internal interface ContentCacheLocation {
    /** The directory the content cache lives in. */
    val root: Path
}
