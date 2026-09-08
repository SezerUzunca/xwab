package com.xwab.app.core.sound.port

/** The physical HTTPS source and stable cache name for one sound. */
data class TrackSource(
    val cacheFileName: String,
    val httpsUrl: String,
)
