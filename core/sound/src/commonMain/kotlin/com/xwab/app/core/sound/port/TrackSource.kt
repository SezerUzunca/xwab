package com.xwab.app.core.sound.port

/** The physical HTTPS source and stable cache name for one sound. */
public data class TrackSource(
    public val cacheFileName: String,
    public val httpsUrl: String,
)
